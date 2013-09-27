(ns protoml.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [protoml.validate :as validate]
            [protoml.io :as io]
            [protoml.parse :as parse]
            [protoml.utils :as utils]))

(defn tutorial []
  "display a tutorial of how to use ProtoML"
  "TODO: tutorial")

(defn summary []
  "displays a summary of ongoing processes"
  "") ; TODO keep statistics to display

(defn new-transform [request]
  "process a request for a new transform"
  (utils/log-err->> request
                    ; request pre-processing
                    validate/request-fields
                    parse/parse-request
                    validate/request-types
                    ; transform
                    io/read-transform
                    validate/transform-definition
                    ; input data
                    io/generate-data-parent-ids
                    io/generate-parent-directories
                    validate/input-directories
                    io/generate-input-prefixes
                    io/generate-input-definitions
                    validate/input-definitions
                    io/generate-input-extensions
                    io/generate-input-paths
                    utils/debug-request
                    utils/return-error
                    validate/input-paths
                    io/read-data
                    validate/data-definition
                    validate/data-compatibility
                    ; transform parameters
                    io/read-parameters
                    validate/transform-parameters
                    ; data-transform compatibility
                    validate/type-check
                    ; output
                    io/generate-transform-id
                    io/generate-directory
                    io/create-directory
                    io/generate-output-paths
                    io/read-random-seed
                    ; performing the operation
                    validate/no-nil
                    validate/new-transform-types
                    utils/return-error
                    ; check point
                    io/process-transform
                    ; post-processing
                    io/make-output-immutable
                    io/write-output-definition
                    )) ; TODO log results/errors in elastic search

(defroutes app-routes
  (GET "/" [] (tutorial))
  (GET "/summary" [] (summary))
  (POST "/new-transform" [& request] (apply str (new-transform request))) ; TODO convert to json
  (POST "/echo" [& request] (println "request: " request) (str request))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
