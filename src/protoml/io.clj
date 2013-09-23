(ns protoml.io
  (:use compojure.core
            [clojure.tools.logging :only (error)]
            [digest :only (md5)])
  (:require [clojure.data.json :as json]
            [clojure.stacktrace :as stacktrace]
            [protoml.shell :as shell]))

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

(defn catch-all [func & args]
    "performs func where all exceptions are caught"
    (try
        (apply func args)
        (catch Throwable e
          (let [st (with-out-str (stacktrace/print-stack-trace e))]
            (error st)
            (to-json {:message (.getMessage e) :stacktrace st})))))

(defn get-transform-id [request]
  "get transform id from hashing input request, and create the needed directory"
  (let [transform-id (md5 (str (apply sorted-map request)))
      directory (parent-directory transform-id)]
    (when (.exists (clojure.java.io/file directory))
      (throw (Exception. "Transform already exists or hash collision")))
    (shell/mkdir-p directory)
    transform-id))

(defn extract-parent-id [datum-id]
  "recovers the id of the parent transform and the index of datum"
  (rest (re-find #"(.*)--(\d+)" datum-id)))

(defn parent-directory [transform-id]
  "get the directory for a specific transform"
  (let [hashed (md5 transform-id)
        partitioned (partition chars-per-directory-level hashed)
        subset (take directory-depth partitioned)
        directory-list (map (partial apply str) subset)
        directory (apply path-join (concat [data-folder] directory-list [transform-id]))]
        directory))

(def CONFIG-FILE "ProtoML_config.json")
(def CONFIG (try (from-json (slurp CONFIG-FILE)) (catch Exception e {}))) ; defaults to empty map if file is not found
(def train-namespace (CONFIG :TrainNamespace "train"))
(def root-directory (CONFIG :RootDir "."))
(def hadoop-mode (CONFIG :HadoopMode false))
(def directory-depth (CONFIG :DirectoryDepth 5))
(def chars-per-directory-level (CONFIG :CharsPerDirectoryLevel 2))

(def data-folder (path-join root-directory "data"))
(def transform-folder (path-join root-directory "transform"))

(defn read-datum [datum-id]
  "locate and read datum's information into a map"
  (let [parent-id (first (extract-parent-id datum-id))
        directory (parent-directory parent-id)
        data-file (path-join directory (str datum-id ".pml-data"))]
        (from-json (slurp data-file))))

(defn read-data [request]
  "locate and read data definitions from data folder"
  (let [data-ids (request :Data)]
    (map read-datum data-ids)))

(defn read-transform [request]
  "read transform definition from json file"
  (let [transform-name (request :TransformName)
        filename (str transform-name ".json")
        full-path (path-join transform-folder filename)]
    (from-json (slurp full-path))))

(defn read-parameters [request]
  "read parameters from json string"
  (let [parameter-str (request :JsonParameters)]
    (from-json parameter-str)))

(defn train-mode? [data-namespace]
  "determines whether or not a namespace is for training"
  (= train-namespace data-namespace))

(defn process-transform [transform-id transform-name data-namespace parameters data]
  "process the data into a transform and make the necessary calls"
  (let [train-mode (= train-namespace data-namespace)]))
