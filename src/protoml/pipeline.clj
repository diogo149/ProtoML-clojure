(ns protoml.pipeline
  (:use [clojure.contrib.map-utils :only [safe-get]])
  (:require [protoml.core :as core]
            [protoml.parse :as parse]
            [protoml.io :as io]
            [protoml.config :as config]
            [protoml.utils :as utils]))

; TODO replace all assoc's with add-key
(defn add-key [key value request]
  "manually set a field in a request"
  (if (contains? request key)
    [nil (str "Request already contains key: " key)]
    [(assoc request key value) nil]))

(defn remove-key [key request]
  "manually remove a field in a request"
  (if (contains? request key)
    [(dissoc request key) nil]
    [nil (str "Request doesn't contain key: " key)]))

(defn parse-request [request]
  "takes in a request and returns a new request with the values parsed"
  (let [parsed (for [[k v] request] [k (parse/parse-rest v)])]
    [(->> parsed
         (apply concat)
         (apply sorted-map)) nil]))


(defn append-transform-id [request]
  "get transform id from hashing input request and add to request"
  (let [transform-id (core/determine-transform-id request)]
    [(assoc request :transform-id transform-id) nil]))

(defn append-directory [request]
  "get the directory of a transform from its request"
  (let [transform-id (safe-get request :transform-id)
        directory (core/determine-parent-directory transform-id)]
    [(assoc request :directory directory) nil]))

(defn create-directory [request]
  "created the needed directory for the transform"
  (let [directory (safe-get request :directory)]
    (if (io/exists? directory) [nil "Transform already exists or hash collision"]
      (let [[value err] (io/create-directory directory)]
        (if (nil? err) [request nil]
          [value err])))))

(defn append-parent-ids [request]
  "add parent ids for the data to a request"
  (let [data-ids (safe-get request :Data)
        parents-and-indices (map core/extract-parent-id data-ids)
        lengths (map count parents-and-indices)]
    (if (some #(not= 2 %) lengths) [nil "Improper data ids"]
      [(assoc request :parent-ids (map first parents-and-indices)) nil])))

(defn append-parent-directories [request]
  "add the directories of the input data to the request"
  (let [parent-ids (safe-get request :parent-ids)
        parent-directories (map core/determine-parent-directory parent-ids)]
    [(assoc request :parent-directories parent-directories) nil]))

(defn append-input-prefixes [request]
  "add the path prefixes (paths without an extension) of the input data to a request"
  (let [parent-directories (safe-get request :parent-directories)
        data-ids (safe-get request :Data)
        zipped (map vector parent-directories data-ids)
        input-prefixes (map #(apply io/path-join %) zipped)]
    (if (= (count parent-directories)
           (count data-ids)
           (count zipped)
           (count input-prefixes))
      [(assoc request :input-prefixes input-prefixes) nil]
      [nil "Improper input prefixes"])))

(defn append-input-definitions [request]
  "add the paths to the data definition files of the input data to a request"
  (let [input-prefixes (safe-get request :input-prefixes)
        input-definitions (map #(str % config/data-extension) input-prefixes)]
    [(assoc request :input-definitions input-definitions) nil]))

(defn append-input-extensions [request]
  "add the available extensions for each input data to the request"
  (let [data-ids (safe-get request :Data)
        parent-directories (safe-get request :parent-directories)
        zipped (map vector parent-directories data-ids)
        data-extensions (map #(apply core/extract-data-extensions %) zipped)]
    [(assoc request :data-extensions data-extensions) nil]))

(defn append-input-paths [request]
  "add the paths of the data files of the input data to a request"
  (let [input-prefixes (safe-get request :input-prefixes)
        data-extensions (safe-get request :data-extensions)
        first-extensions (map first data-extensions)
        zipped (map vector input-prefixes first-extensions)
        input-paths (for [[prefix ext] zipped] (str prefix ext))]
    [(assoc request :input-paths input-paths) nil]
    )) ; TODO have file formatter intelligently choose format

(defn append-random-seed [request]
  "read random seed, if available, else generate one"
  (let [transform-id (safe-get request :transform-id)
        random-seed (get request :RandomSeed (core/generate-random-seed transform-id))]
    [(assoc request :random-seed random-seed) nil]))

(defn read-data [request]
  "locate and read data definitions from data folder"
  (let [input-definitions (safe-get request :input-definitions)
        data-with-errors (map io/safe-open-json input-definitions)
        final-error (apply (partial utils/combine-errors nil) data-with-errors)
        data (map first data-with-errors)]
    (if (nil? (second final-error))
      [(assoc request :data data) nil]
      final-error)))

(defn read-transform [request]
  "read transform definition from json file"
  (let [transform-name (safe-get request :TransformName)
        transform-file (str transform-name config/transform-extension)
        full-path (io/path-join config/transform-folder transform-file)
        [transform error] (io/safe-open-json full-path)]
    (if (nil? error) [(assoc request :transform transform)]
      [nil error])))

(defn append-parameters [request]
  "read parameters from json string"
  (let [parameters (safe-get request :JsonParameters)]
    [(assoc request :parameters parameters) nil]))

(defn append-output-num [request]
  "generate number of outputs"
  (let [transform (safe-get request :transform)
        output-num (count (safe-get transform :Output))]
    [(assoc request :output-num output-num) nil]))

(defn append-transform-path [request]
  "add the path to the executable transform to the request"
  (let [transform-name (safe-get request :TransformName)
        transform-path (io/path-join config/transform-folder transform-name)]
    [(assoc request :transform-path transform-path) nil]))

(defn append-output-prefixes [request]
  "generate output prefixes and add to request"
  (let [output-num (safe-get request :output-num)
        transform-id (safe-get request :transform-id)
        directory (safe-get request :directory)
        output-prefixes (core/to-output-prefixes directory transform-id output-num)]
    [(assoc request :output-prefixes output-prefixes) nil]))

(defn append-output-extensions [request]
  (let [transform (safe-get request :transform)
        output (safe-get transform :Output)
        output-extensions (map #(safe-get % :Extension) output)]
    [(assoc request :output-extensions output-extensions) nil]))

(defn append-output-paths [request]
  (let [output-prefixes (safe-get request :output-prefixes)
        output-extensions (safe-get request :output-extensions)
        output-paths (map #(apply str %) (map vector
                                              output-prefixes
                                              output-extensions))]
    [(assoc request :output-paths output-paths) nil]))

(defn append-output-definitions [request]
  (let [output-prefixes (safe-get request :output-prefixes)
        output-definitions (map #(str % config/data-extension) output-prefixes)]
    [(assoc request :output-definitions output-definitions) nil]))

(defn append-output-definitions-content [request]
  (let [transform (safe-get request :transform)
        output-definitions-content (safe-get transform :Output)]
    [(assoc request :output-definitions-content output-definitions-content) nil]))

(defn append-model-path [request]
  "add the path where the serialized model should go"
  (let [directory (safe-get request :directory)
        transform-id (safe-get request :transform-id)
        model-file (str transform-id config/model-extension)
        model-path (io/path-join directory model-file)]
    [(assoc request :model-path model-path)]))

(defn append-train-mode [request]
  "add a flag indicating whether or not train mode is active"
  (let [data-namespace (safe-get request :DataNamespace)
        train-mode (core/train-mode? data-namespace)]
    [(assoc request :train-mode train-mode)]))

(defn make-output-immutable [request]
  "makes the output of a transform immutable (read only)"
  (let [output-paths (safe-get request :output-paths)]
    (utils/combine-errors request (map io/make-read-only output-paths))))

(defn make-model-immutable [request]
  "makes the model of a transform immutable (read only)"
  (let [model-path (safe-get request :model-path)
        [_ error] (io/make-read-only model-path)]
    (if (nil? error) [request nil]
      [nil error])))

(defn write-output-definitions [request]
  "writes the json files for the output data"
  (let [output-definitions (safe-get request :output-definitions)
        output-definitions-content (safe-get request :output-definitions-content)
        zipped (map vector output-definitions output-definitions-content)
        errors (doall
                 (for [[output-definition output-definition-content] zipped]
                   (io/safe-write-json output-definition output-definition-content)
                   ))]
    (utils/combine-errors request errors)))

(defn make-output-definitions-immutable [request]
  "makes the output definitions of a transform immutable (read only)"
  (let [output-definitions (safe-get request :output-definitions)]
    (utils/combine-errors request (map io/make-read-only output-definitions))))
