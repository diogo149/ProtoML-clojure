(ns protoml.parse
  (:use compojure.core
        [clojure.contrib.map-utils :only [safe-get]])
  (:require [clojure.data.json :as json]))

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

(defn to-json [m] (json/write-str m :value-fn json-value-writer :key-fn name))

(defn from-json [s] (json/read-str s :value-fn json-value-reader :key-fn keyword))

(defn parse-rest [value]
  "tries to parse an input string as json"
  (try (from-json value)
    (catch Throwable e value)))

(defn parse-request [request]
  "takes in a request and returns a new request with the values parsed"
  (let [parsed (for [[k v] request] [k (parse-rest v)])]
    [(->> parsed
         (apply concat)
         (apply sorted-map)) nil]))
