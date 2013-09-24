(ns protoml.io
  (:use compojure.core
            [clojure.tools.logging :only (info error)]
            [digest :only (md5)])
  (:require [clojure.data.json :as json]
            [clojure.stacktrace :as stacktrace]
            [protoml.shell :as shell]
            [protoml.utils :as utils]))

(def data-extension ".pml-data")
(def model-extension ".pml-model")

(defn path-join [& args]
  "joins path arguments into a single string"
  (clojure.string/replace (apply str (interpose "," args)) #"/+" "/"))

(defn json-value-reader [key value]
  ; (if (= key :date)
  ;   (java.sql.Date/valueOf value)
  ;   value)
    value
  )

(defn json-value-writer [key value]
  ; (if (= key :date)
  ;   (str (java.sql.Date. (.getTime value)))
  ;   value)
    value
)

(defn to-json [m] (json/write-str m :value-fn json-value-writer
                :key-fn name))

(defn from-json [s] (json/read-str s :value-fn json-value-reader :key-fn keyword))

(def CONFIG-FILE "ProtoML_config.json")
(def CONFIG (try (from-json (slurp CONFIG-FILE)) (catch Throwable e {}))) ; defaults to empty map if file is not found
(def train-namespace (CONFIG :TrainNamespace "train"))
(def root-directory (CONFIG :RootDir "."))
(def hadoop-mode (CONFIG :HadoopMode false))
(def directory-depth (CONFIG :DirectoryDepth 5))
(def chars-per-directory-level (CONFIG :CharsPerDirectoryLevel 2))

(def data-folder (path-join root-directory "data"))
(def transform-folder (path-join root-directory "transform"))

(defn parent-directory [transform-id]
  "get the directory for a specific transform"
  (let [hashed (md5 transform-id)
        partitioned (partition chars-per-directory-level hashed)
        subset (take directory-depth partitioned)
        directory-list (map (partial apply str) subset)
        directory (apply path-join (concat [data-folder] directory-list [transform-id]))]
        directory))

(defn get-transform-id [request]
  "get transform id from hashing input request, and create the needed directory; returns [request error]"
  (let [transform-id (md5 (str (apply sorted-map request)))
      directory (parent-directory transform-id)]
      (if (.exists (clojure.java.io/file directory)) [nil "Transform already exists or hash collision"]
        (let [[_ err] (shell/mkdir-p directory)]
          (if (nil? err) [(assoc request :transform-id transform-id) nil]
                         [nil err])))))

(defn extract-parent-id [datum-id]
  "recovers the id of the parent transform and the index of datum"
  (rest (re-find #"(.*)--(\d+)" datum-id)))

(defn to-datum-id [parent-id index]
  "creates a datum id from a parent id and index"
  (str parent-id "--" index))

(defn read-datum [datum-id]
  "locate and read datum's information into a map"
  (let [parent-id (first (extract-parent-id datum-id))
        directory (parent-directory parent-id)
        data-file (path-join directory (str datum-id data-extension))]
        (from-json (slurp data-file))))

(defn read-data [request]
  "locate and read data definitions from data folder"
  (let [data-ids (request :Data)
         data (map read-datum data-ids)]
        [(assoc request :data data) nil]))

(defn read-transform [request]
  "read transform definition from json file"
  (let [transform-name (request :TransformName)
        filename (str transform-name ".json")
        full-path (path-join transform-folder filename)]
    (from-json (slurp full-path)))) ; TODO recursive transform look up (for templates

(defn read-parameters [request]
  "read parameters from json string"
  (let [parameter-str (request :JsonParameters)
        parameters (from-json parameter-str)]
        [(assoc request :parameters parameters) nil]))

(defn read-random-seed [request]
   "read random seed, if available, else generate one"
   (let [transform-id (request :transform-id)
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
  (let [transform-id (request :transform-id)
        transform-name (request :TransformName)
        transform (request :transform)
        data-namespace (request :DataNamespace)
        parameters (request :parameters)
        data (request :data)
        train-mode (train-mode? data-namespace)
        transform-path (path-join transform-folder transform-name)
        output-num (count (transform :output))
        input-paths (map :full-path data)
        directory (parent-directory transform-id)
        output-paths (to-output-paths directory transform-id output-num)
        model-path (path-join directory (str transform-id model-extension))
        random-seed (request :random-seed)]
    (call-transform transform-path parameters input-paths output-paths model-path train-mode random-seed)
    )) ; TODO add creation of these to another node in the pipeline

(defn make-output-immutable [request]
  "makes the output of a transform immutable (read only)"
  (let [model-path (request :model-path)
        output-paths (request :output-paths)]
        (utils/combine-errors request (map shell/make-read-only (conj output-paths model-path)))))

(defn write-output-definition [request]
  "writes the json files for the output data"
  (let [output-paths (request :output-paths)
        transform (request :transform)
        output-def (transform :Output)
        directory (request :directory)])
  [request nil])
