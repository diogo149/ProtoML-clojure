(ns protoml.validate
  (:use compojure.core))

(defn request-fields [request]
    "if the request for a new transform is invalid, returns an error"
    (if (every? #(contains? request %) [:TransformName :DataNamespace :JsonParameters :Data :Tags]) [request nil]
        [nil "Missing parameters in transform request"]))

(defn type-check [request]
    "if the request has incompatible types for transform and input, returns an error"
    (let [transform (request :transform)
        data (request :data)]
        ; TODO
        [request nil]))

(defn transform-parameters [request]
    "if the request has incompatible parameters for the transform, returns an error"
    (let [transform (request :transform)
        parameters (request :parameters)]
        ; TODO check values and also that the names aren't the reserved names (input output trainmode model)
        [request nil]
        ))

(defn transform-definition [request]
    "if the transform definition is invalid, returns an error"
    (let [transform (request :transform)]
        ; TODO
        [request nil]
        ))

(defn datum-definition [datum]
    "if the input datum definition is invalid, returns false"
    true) ; TODO

(defn data-definition [request]
    "if the input data definition is invalid, returns an error"
    (let [data (request :data)]
        (if (every? true? (map datum-definition data)) [request nil]
            [nil "Invalid input data definition"])))

(defn data-compatibility [request]
    "if the input data is incompatible, returns an error"
    (let [data (request :data)]
        (if (apply = (map :NRows data)) [request nil]
            [nil "Incompatible data"])))

(defn no-nil [input-map]
    "if the input-map has nil values, returns an error"
    (let [values (map second input-map)]
        (if (some nil? values) [nil "Input map has nil values."]
        [input-map nil])))

(defn has-necessities [request]
    "validates that a request has all the necessary fields to perform create the transform"
    (if (every? #(contains? request %) [:TransformName :DataNamespace :JsonParameters :Data :Tags]) [request nil] ; TODO
        [nil "Missing parameters in transform request"]))
