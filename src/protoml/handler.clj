(ns protoml.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [protoml.validate :as validate]
            [protoml.io :as io]
            [protoml.utils :as utils]))

(defn summary []
    "displays a summary of ongoing processes"
    "") ; keep statistics to display

(defn new-transform [request]
    "process a request for a new transform"
    (utils/log-err->> request
        validate/request-fields
        io/get-transform-id
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
        io/process-transform
        io/make-output-immutable
        io/write-output-definition
        ; TODO log results in elastic search
        ))

(defroutes app-routes
  (GET "/summary" [] (summary))
  (POST "/new-transform" [& request] (apply str (new-transform request))) ; TODO convert to json
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
