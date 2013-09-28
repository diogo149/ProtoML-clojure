(ns protoml.validate
  (:use compojure.core
        [clojure.contrib.map-utils :only [safe-get]])
  (:require [me.raynes.fs :as fs]))

(defn request-fields [request]
  "if the request for a new transform is invalid, returns an error"
  (if (every? #(contains? request %) [:TransformName :DataNamespace :JsonParameters :Data :Tags]) [request nil]
    [nil "Missing parameters in transform request"]))

(defn request-types [request]
  "if the values of a request have the wrong type, returns an error"
  [request nil]) ; TODO make sure data and tags are vectors, and json parameters is map, and name and namespace are strings

(defn type-check [request]
  "if the request has incompatible types for transform and input, returns an error"
  (let [transform (safe-get request :transform)
        data (safe-get request :data)]
    ; TODO
    [request nil]))

(defn transform-parameters [request]
  "if the request has incompatible parameters for the transform, returns an error"
  (let [transform (safe-get request :transform)
        parameters (safe-get request :parameters)]
    ; TODO check values and also that the names aren't the reserved names (input output trainmode model)
    [request nil]
    ))

(defn transform-definition [request]
  "if the transform definition is invalid, returns an error"
  (let [transform (safe-get request :transform)]
    ; TODO
    [request nil]
    ))

(defn datum-definition [datum]
  "if the input datum definition is invalid, returns false"
  true) ; TODO

(defn data-definition [request]
  "if the input data definition is invalid, returns an error"
  (let [data (safe-get request :data)]
    (if (every? true? (map datum-definition data)) [request nil]
      [nil "Invalid input data definition"])))

(defn data-compatibility [request]
  "if the input data is incompatible, returns an error"
  (let [data (safe-get request :data)
        rows (map #(safe-get % :NRows) data)
        zero-rows (= 0 (count rows))]
    (if (or zero-rows (apply = (map :NRows data))) [request nil]
      [nil "Incompatible data"])))

(defn no-nil [input-map]
  "if the input-map has nil values, returns an error"
  (let [values (map second input-map)]
    (if (some nil? values) [nil "Input map has nil values."]
      [input-map nil])))

(defn new-transform-types [request]
  "if the types of the data for a new transform are invalid, returns an error"
  ; TODO
  [request nil])

(defn input-directories [request]
  "validate that all input data directories exist"
  (let [parent-directories (safe-get request :parent-directories)
        valid (every? true? (map fs/directory? parent-directories))]
    (if valid [request nil]
      [nil "Invalid input data directory"])))

(defn input-definitions [request]
  "validate that all input data definitions exist"
  ; TODO
  [request nil])

(defn input-definitions [request]
  "validate that all the data extensions are valid (at least one per data)"
  ; TODO
  [request nil])

(defn input-paths [request]
  "validate that all input data paths exist"
  ; TODO
  [request nil])

(defn transform-path [request]
  "validate that the transform path exists and is executable"
  ; TODO
  [request nil])

(defn random-seed [request]
  "validate that the random seed is made up only of digits (positive integer)"
  ; TODO
  [request nil])
