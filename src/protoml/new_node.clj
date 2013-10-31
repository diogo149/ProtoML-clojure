(ns protoml.new-transform
  (:use [clojure.contrib.map-utils :only [safe-get]])
  (:require [protoml.core :as core]
            [protoml.validate :as validate]
            [protoml.pipeline :as pipeline]
            [protoml.utils :as utils])
  )

(def input-fields
  "map of the input fields names to validation functions for those fields"
  {:Transform string?
   :Data validate/strings?
   :Parameters map?
   :RandomSeed core/valid-random-seed?
   :Alias string?
   :Tags validate/strings?})

(def validate
  -input-fields (partial validate/validate-input-fields
                                    (keys input-fields)))

(defn input-validation [request]
  (utils/err->> request
                (partial validate/validate-input-fields
                         (keys input-fields))
                pipeline/parse-request
                (partial validate/request-types input-fields)))
