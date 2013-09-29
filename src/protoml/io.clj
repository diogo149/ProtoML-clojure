(ns protoml.io
  (:use compojure.core
        [clojure.tools.logging :only (info error)]
        [digest :only (md5)]
        [clojure.contrib.map-utils :only [safe-get]])
  (:require [clojure.stacktrace :as stacktrace]
            [protoml.shell :as shell]
            [protoml.parse :as parse]
            [protoml.utils :as utils]
            [me.raynes.fs :as fs]
            [protoml.core :as core]))

(def data-extension ".pml-data")
(def model-extension ".pml-model")
(def transform-extension ".json")

(defn path-join [& args]
  "joins path arguments into a single string"
  (clojure.string/replace (apply str (interpose "/" args)) #"/+" "/"))

(defn safe-slurp [filename]
  "tries to slurp, and if this fails, returns an error"
  (utils/exception-to-error slurp filename))

(defn safe-spit [filename string]
  (utils/exception-to-error spit filename string))

(defn safe-open-json [filename]
  "open and parse json"
  (utils/err->> filename
                safe-slurp
                parse/safe-from-json))

(defn safe-write-json [filename object]
  "convert to json and write to file"
  (utils/err->> object
                parse/safe-to-json
                (partial safe-spit filename)))

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
      (let [extensions (map fs/extension files)
            non-nil (filter (complement nil?) extensions)
            data-extensions (filter #(not= % data-extension) non-nil)]
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
        zipped (map vector input-prefixes first-extensions)
        input-paths (for [[prefix ext] zipped] (str prefix ext))]
    [(assoc request :input-paths input-paths) nil]
    )) ; TODO have file formatter intelligently choose format

(defn read-data [request]
  "locate and read data definitions from data folder"
  (let [input-definitions (safe-get request :input-definitions)
        data-with-errors (map safe-open-json input-definitions)
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
      [nil error])))

(defn read-parameters [request]
  "read parameters from json string"
  (let [parameters (safe-get request :JsonParameters)]
    [(assoc request :parameters parameters) nil]))

(defn read-random-seed [request]
  "read random seed, if available, else generate one"
  (let [transform-id (safe-get request :transform-id)
        random-seed (get request :RandomSeed (Math/abs (hash transform-id)))]
    [(assoc request :random-seed random-seed) nil]))

(defn train-mode? [data-namespace]
  "determines whether or not a namespace is for training"
  (= train-namespace data-namespace))

(defn to-output-prefixes [directory transform-id output-num]
  "creates the prefixes for the output of a transform"
  (for [i (range 1 (inc output-num))]
    (path-join directory (to-datum-id transform-id i))))

(defn set-output-num [output-num request]
  "manual set the number of outputs"
  [(assoc request :output-num output-num) nil])

(defn generate-output-num [request]
  "generate number of outputs"
  (let [transform (safe-get request :transform)
        output-num (count (safe-get transform :Output))]
    [(assoc request :output-num output-num) nil]))

(defn generate-output-prefixes [request]
  "generate output prefixes and add to request"
  (let [output-num (safe-get request :output-num)
        transform-id (safe-get request :transform-id)
        directory (safe-get request :directory)
        output-prefixes (to-output-prefixes directory transform-id output-num)]
    [(assoc request :output-prefixes output-prefixes) nil]))

(defn manual-output-extension [request]
  "set the output extension for manual input"
  (let [extension (safe-get request :extension)
        output-extensions [extension]]
    [(assoc request :output-extensions output-extensions) nil]))

(defn generate-output-extensions [request]
  (let [transform (safe-get request :transform)
        output (safe-get transform :Output)
        output-extensions (map #(safe-get % :Extension) output)]
    [(assoc request :output-extensions output-extensions) nil]))

(defn generate-output-paths [request]
  (let [output-prefixes (safe-get request :output-prefixes)
        output-extensions (safe-get request :output-extensions)
        output-paths (map #(apply str %) (map vector
                                              output-prefixes
                                              output-extensions))]
    [(assoc request :output-paths output-paths) nil]))

(defn generate-output-definitions [request]
  (let [output-prefixes (safe-get request :output-prefixes)
        output-definitions (map #(str % data-extension) output-prefixes)]
    [(assoc request :output-definitions output-definitions) nil]))

(defn generate-output-definitions-content [request]
  (let [transform (safe-get request :transform)
        output-definitions-content (safe-get transform :Output)]
    [(assoc request :output-definitions-content output-definitions-content) nil]))

(defn manual-output-definitions-content [request]
  (let [NCols (safe-get request :NCols)
        Extension (safe-get request :extension)
        Type (safe-get request :Type)
        output-definitions-content [{:Type Type :Ncols NCols :Extension Extension}]]
    [(assoc request :output-definitions-content output-definitions-content) nil]))

(defn generate-transform-path [request]
  "add the path to the executable transform to the request"
  (let [transform-name (safe-get request :TransformName)
        transform-path (path-join transform-folder transform-name)]
    [(assoc request :transform-path transform-path) nil]))

(defn generate-model-path [request]
  "add the path where the serialized model should go"
  (let [directory (safe-get request :directory)
        transform-id (safe-get request :transform-id)
        model-file (str transform-id model-extension)
        model-path (path-join directory model-file)]
    [(assoc request :model-path model-path)]))

(defn generate-train-mode [request]
  "add a flag indicating whether or not train mode is active"
  (let [data-namespace (safe-get request :DataNamespace)
        train-mode (train-mode? data-namespace)]
    [(assoc request :train-mode train-mode)]))

(defn call-transform [transform-path parameters input-paths output-paths model-path train-mode random-seed]
  "calls the transform file to perform the transform"
  (let [input-map (merge {:input input-paths :output output-paths :model model-path :trainmode train-mode :seed random-seed} parameters)]
    (shell/generic-call transform-path input-map)))

(defn process-transform [request]
  "process the data into a transform and make the necessary calls"
  (let [transform-path (safe-get request :transform-path)
        parameters (safe-get request :parameters)
        input-paths (safe-get request :input-paths)
        output-paths (safe-get request :output-paths)
        model-path (safe-get request :model-path)
        transform (safe-get request :transform)
        train-mode (safe-get request :train-mode)
        random-seed (safe-get request :random-seed)
        [transform-output error] (call-transform transform-path
                                                 parameters
                                                 input-paths
                                                 output-paths
                                                 model-path
                                                 train-mode
                                                 random-seed)]
    (if (nil? error) [(assoc request :transform-output transform-output) nil]
      [nil error])))

(defn make-output-immutable [request]
  "makes the output of a transform immutable (read only)"
  (let [output-paths (safe-get request :output-paths)]
    (utils/combine-errors request (map shell/make-read-only output-paths))))

(defn make-model-immutable [request]
  "makes the model of a transform immutable (read only)"
  (let [model-path (safe-get request :model-path)
        [_ error] (shell/make-read-only model-path)]
    (if (nil? error) [request nil]
      [nil error])))

(defn write-output-definitions [request]
  "writes the json files for the output data"
  (let [output-definitions (safe-get request :output-definitions)
        output-definitions-content (safe-get request :output-definitions-content)
        zipped (map vector output-definitions output-definitions-content)
        errors (doall
                 (for [[output-definition output-definition-content] zipped]
                   (safe-write-json output-definition output-definition-content)
                   ))]
    (utils/combine-errors request errors)))

(defn manual-generate-extension [request]
  "parse the extension from the given filepath"
  (let [filepath (safe-get request :Filepath)
        extension (fs/extension filepath)]
    (if (nil? extension) [nil "Filepath has no extension"]
      [(assoc request :extension extension) nil])))

(defn process-manual-input [request]
  "process the manual input by copying the file at the given filepath to the proper location in the data folder"
  (let [filepath (safe-get request :Filepath)
        [output-path] (safe-get request :output-paths)
        [_ error] (utils/exception-to-error fs/copy filepath output-path)]
    (if (nil? error) [request error]
      [nil error])))
