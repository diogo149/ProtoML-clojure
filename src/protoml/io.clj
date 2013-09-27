(ns protoml.io
  (:use compojure.core
        [clojure.tools.logging :only (info error)]
        [digest :only (md5)]
        [clojure.contrib.map-utils :only [safe-get]])
  (:require [clojure.stacktrace :as stacktrace]
            [protoml.shell :as shell]
            [protoml.parse :as parse]
            [protoml.utils :as utils]
            [me.raynes.fs :as fs]))

(def data-extension ".pml-data")
(def model-extension ".pml-model")
(def transform-extension ".json")

(defn path-join [& args]
  "joins path arguments into a single string"
  (clojure.string/replace (apply str (interpose "/" args)) #"/+" "/"))

(defn safe-slurp [filename]
  "tries to slurp, and if this fails, returns an error"
  (utils/exception-to-error slurp filename))

(def CONFIG-FILE "ProtoML_config.json")
(def CONFIG (try (parse/from-json (slurp CONFIG-FILE)) (catch Throwable e {}))) ; defaults to empty map if file is not found
(def train-namespace (CONFIG :TrainNamespace "train"))
(def root-directory (CONFIG :RootDir "."))
(def hadoop-mode (CONFIG :HadoopMode false))
(def directory-depth (CONFIG :DirectoryDepth 5))
(def chars-per-directory-level (CONFIG :CharsPerDirectoryLevel 2))

(def data-folder (path-join root-directory "data"))
(def transform-folder (path-join root-directory "transform"))

(defn generate-transform-id [request]
  "get transform id from hashing input request, and create the needed directory; returns [request error]"
  (let [transform-id (->> request
                          vec
                          (apply concat)
                          (apply sorted-map)
                          str
                          md5)]
    [(assoc request :transform-id transform-id) nil]))

(defn parent-directory [transform-id]
  "get the directory for a specific transform"
  (let [hashed (md5 transform-id)
        partitioned (partition chars-per-directory-level hashed)
        subset (take directory-depth partitioned)
        directory-list (map (partial apply str) subset)
        directory (apply path-join (concat [data-folder] directory-list [transform-id]))]
    directory))

(defn generate-directory [request]
  "get the directory of a transform from its request"
  (let [directory (parent-directory (safe-get request :transform-id))]
    [(assoc request :directory directory) nil]))

(defn create-directory [request]
  "created the needed directory for the transform"
  (let [directory (safe-get request :directory)]
    (if (.exists (clojure.java.io/file directory)) [nil "Transform already exists or hash collision"]
      (let [[value err] (shell/mkdir-p directory)]
        (if (nil? err) [request nil]
          [value err])))))

(defn extract-parent-id [datum-id]
  "recovers the id of the parent transform and the index of datum"
  (rest (re-find #"^([0-9a-f]{32})--(\d+)$" datum-id)))

(defn to-datum-id [parent-id index]
  "creates a datum id from a parent id and index"
  (str parent-id "--" index))

(defn generate-data-parent-ids [request]
  "add parent ids for the data to a request"
  (let [data-ids (safe-get request :Data)
        parents-and-indices (map extract-parent-id data-ids)
        lengths (map count parents-and-indices)]
    (if (some #(not= 2 %) lengths) [nil "Improper data ids"]
      [(assoc request :parent-ids (map first parents-and-indices)) nil])))

(defn generate-parent-directories [request]
  "add the directories of the input data to the request"
  (let [parent-ids (safe-get request :parent-ids)
        parent-directories (map parent-directory parent-ids)]
    [(assoc request :parent-directories parent-directories) nil]))

(defn generate-input-prefixes [request]
  "add the path prefixes (paths without an extension) of the input data to a request"
  (let [parent-directories (safe-get request :parent-directories)
        data-ids (safe-get request :Data)
        zipped (map vector parent-directories data-ids)
        input-prefixes (map #(apply path-join %) zipped)]
    [(assoc request :input-prefixes input-prefixes) nil]))

(defn generate-input-definitions [request]
  "add the paths to the data definition files of the input data to a request"
  (let [input-prefixes (safe-get request :input-prefixes)
        input-definitions (map #(str % data-extension) input-prefixes)]
    [(assoc request :input-definitions input-definitions) nil]))

(defn extract-data-extensions [directory data-id]
  "returns a collection of extensions available for a certain data-id"
  (let [files (fs/list-dir directory)]
    (if (nil? files) (throw (Exception. "Invalid directory to extract data extensions from"))
      (let [pattern-str (str "^" data-id "(\\..*)$")
        pattern (re-pattern pattern-str)
        mapped (map #(re-find pattern %) files)
        matches (filter (complement nil?) mapped)
        extensions (map second matches)
        data-extensions (filter #(not= % data-extension))]
        data-extensions))))

(defn generate-input-extensions [request]
  "add the available extensions for each input data to the request"
  (let [data-ids (safe-get request :Data)
        parent-directories (safe-get request :parent-directories)
        zipped (map vector parent-directories data-ids)
        data-extensions (map #(apply extract-data-extensions %) zipped)]
    [(assoc request :data-extensions data-extensions) nil]
    ))

(defn generate-input-paths [request]
  "add the paths of the data files of the input data to a request"
  (let [input-prefixes (safe-get request :input-prefixes)
        data-extensions (safe-get request :data-extensions)
        first-extensions (map first data-extensions)
        zipped (map vector input-prefixes data-extensions)
        input-paths (for [[prefix ext] zipped] (str prefix ext))]
    [(assoc request :input-paths input-paths) nil]
    )) ; TODO have file formatter intelligently choose format

(defn read-datum [data-file]
  "locate and read datum's information into a map"
  (utils/err->> data-file
                safe-slurp
                parse/safe-from-json))

(defn read-data [request]
  "locate and read data definitions from data folder"
  (let [input-definitions (safe-get request :input-definitions)
        data-with-errors (map read-datum input-definitions)
        final-error (apply (partial utils/combine-errors nil) data-with-errors)
        data (map first data-with-errors)]
    (if (nil? (second final-error)) [(assoc request :data data) nil]
      final-error)))

(defn read-transform [request]
  "read transform definition from json file"
  (let [transform-name (safe-get request :TransformName)
        transform-file (str transform-name transform-extension)
        full-path (path-join transform-folder transform-file)
        [transform error] (safe-open-json full-path)]
    (if (nil? error) [(assoc request :transform transform)]
      [nil error]))) ; TODO recursive transform look up (for templates

(defn read-parameters [request]
  "read parameters from json string"
  (let [parameters (safe-get request :JsonParameters)]
    [(assoc request :parameters parameters) nil]))

(defn read-random-seed [request]
  "read random seed, if available, else generate one"
  (let [transform-id (safe-get request :transform-id)
        random-seed (get request :RandomSeed (hash transform-id))]
    [(assoc request :random-seed random-seed) nil]))

(defn train-mode? [data-namespace]
  "determines whether or not a namespace is for training"
  (= train-namespace data-namespace))

(defn to-output-paths [directory transform-id output-num]
  "creates the paths for the output of a transform"
  (for [i (range output-num)] (path-join directory (str (to-datum-id transform-id i) data-extension))))

(defn generate-output-paths [request]
  "generate output paths and add to request"
  (let [transform (safe-get request :transform)
        output-num (count (safe-get transform :Output))
        transform-id (safe-get request :transform-id)
        directory (safe-get request :directory)
        output-paths (to-output-paths directory transform-id output-num)]
    [(assoc request :output-paths output-paths) nil]))

(defn call-transform [transform-path parameters input-paths output-paths model-path train-mode random-seed]
  "calls the transform file to perform the transform"
  (let [input-map (merge parameters {:input input-paths :output output-paths :model model-path :trainmode train-mode :seed random-seed})]
    (shell/generic-call transform-path input-map)))

(defn process-transform [request]
  "process the data into a transform and make the necessary calls"
  (let [transform-id (safe-get request :transform-id)
        transform-name (safe-get request :TransformName)
        transform-path (path-join transform-folder transform-name)
        transform (safe-get request :transform)
        data-namespace (safe-get request :DataNamespace)
        parameters (safe-get request :parameters)
        data (safe-get request :data)
        train-mode (train-mode? data-namespace)
        input-paths (map :full-path data)
        directory (safe-get request :directory)
        output-paths (safe-get request :output-paths)
        model-path (path-join directory (str transform-id model-extension))
        random-seed (safe-get request :random-seed)]
    (call-transform transform-path parameters input-paths output-paths model-path train-mode random-seed)
    )) ; TODO add creation of these to another node in the pipeline

(defn make-output-immutable [request]
  "makes the output of a transform immutable (read only)"
  (let [model-path (safe-get request :model-path)
        output-paths (safe-get request :output-paths)]
    (utils/combine-errors request (map shell/make-read-only (conj output-paths model-path)))))

(defn write-output-definition [request]
  "writes the json files for the output data"
  (let [output-paths (safe-get request :output-paths)
        transform (safe-get request :transform)
        output-def (safe-get transform :Output)
        directory (safe-get request :directory)])
  [request nil])
