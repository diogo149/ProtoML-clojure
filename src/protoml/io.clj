(ns protoml.io
  (:use compojure.core
        [clojure.tools.logging :only (info error)]
        [digest :only (md5)]
        [clojure.contrib.map-utils :only [safe-get]])
  (:require [clojure.stacktrace :as stacktrace]
            [protoml.shell :as shell]
            [protoml.parse :as parse]
            [protoml.utils :as utils]))

(def data-extension ".pml-data")
(def model-extension ".pml-model")

(defn path-join [& args]
  "joins path arguments into a single string"
  (clojure.string/replace (apply str (interpose "/" args)) #"/+" "/"))

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

(defn parent-directory [transform-id] ; TODO make sure nothing else calls thing other than generate-directory
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

(defn read-datum [datum-id]
  "locate and read datum's information into a map"
  (let [parent-id (first (extract-parent-id datum-id))
        directory (parent-directory parent-id)
        data-file (path-join directory (str datum-id data-extension))]
    (parse/from-json (slurp data-file))))

(defn read-data [request]
  "locate and read data definitions from data folder"
  (let [data-ids (safe-get request :Data)
        data (map read-datum data-ids)]
    [(assoc request :data data) nil]))

(defn read-transform [request]
  "read transform definition from json file"
  (let [transform-name (safe-get request :TransformName)
        filename (str transform-name ".json")
        full-path (path-join transform-folder filename)]
    (parse/from-json (slurp full-path)))) ; TODO recursive transform look up (for templates

(defn read-parameters [request]
  "read parameters from json string"
  (let [parameter-str (safe-get request :JsonParameters)
        parameters (parse/from-json parameter-str)]
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

(defn call-transform [transform-path parameters input-paths output-paths model-path train-mode random-seed]
  "calls the transform file to perform the transform"
  (let [input-map (merge parameters {:input input-paths :output output-paths :model model-path :trainmode train-mode :seed random-seed})]
    (shell/generic-call transform-path input-map)))

(defn process-transform [request]
  "process the data into a transform and make the necessary calls"
  (let [transform-id (safe-get request :transform-id)
        transform-name (safe-get request :TransformName)
        transform (safe-get request :transform)
        data-namespace (safe-get request :DataNamespace)
        parameters (safe-get request :parameters)
        data (safe-get request :data)
        train-mode (train-mode? data-namespace)
        transform-path (path-join transform-folder transform-name)
        output-num (count (transform :output))
        input-paths (map :full-path data)
        directory (parent-directory transform-id)
        output-paths (to-output-paths directory transform-id output-num)
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
