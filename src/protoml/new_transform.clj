(ns protoml.new-transform
  (:use [clojure.contrib.map-utils :only [safe-get]])
  (:require [protoml.core :as core]))

(defn call-transform [transform-path parameters input-paths output-paths model-path train-mode random-seed]
  "calls the transform file to perform the transform"
  (let [input-map (merge {:input input-paths :output output-paths :model model-path :trainmode train-mode :seed random-seed} parameters)]
    (core/generic-call transform-path input-map)))

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
