(ns protoml.validate
  (:use compojure.core))

(defn request-fields [request]
    "throws an exception if the request for a new transform is invalid"
    (cond (not (every? #(contains? request %) [:TransformName :DataNamespace :JsonParameters :Data :Tags]))
        (throw (Exception. "Missing parameters in transform request"))))

(defn type-check [transform data]
    "throws an exception if the request has incompatible types for transform and input"
    nil) ; TODO

(defn transform-parameters [transform parameters]
    "throws an exception if the request has incompatible parameters for the transform"
    nil) ; TODO check values and also that the names aren't the reserved names (input output trainmode model)

(defn transform-definition [transform]
    "throws an exception if the transform definition is invalid"
    nil) ; TODO

(defn data-definition [data]
    "throws an exception if the input data definition is invalid"
    (map datum-definition data))

(defn datum-definition [datum]
    "throws an exception if the input datum definition is invalid"
    nil) ; TODO

(defn data-compatibility [data]
    "throws an exception if the input data is incompatible"
    nil)
