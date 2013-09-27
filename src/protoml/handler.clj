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
                    validate/request-fields
                    parse/parse-request
                    validate/request-types
                    io/generate-transform-id
                    io/generate-directory
                    io/create-directory
                    io/generate-data-parent-ids
                    io/read-data
                    io/read-transform
                    io/read-parameters
                    io/read-random-seed
                    validate/data-definition
                    validate/data-compatibility
                    validate/transform-definition
                    validate/type-check
                    validate/transform-parameters
                    validate/no-nil
                    utils/return-error
                    ; utils/debug-request
                    ; utils/debug-request
                    ; check point (:
                    io/process-transform
                    io/make-output-immutable
                    io/write-output-definition
                    ; TODO log results in elastic search
                    ))

(defroutes app-routes
  (GET "/" [] (tutorial))
  (GET "/summary" [] (summary))
  (POST "/new-transform" [& request] (apply str (new-transform request))) ; TODO convert to json
  (POST "/echo" [& request] (println "request: " request) (str request))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
