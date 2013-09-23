(ns protoml.handler
  (:use compojure.core
    [clojure.tools.logging :only (info error)])
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [protoml.validate :as validate]
            [protoml.io :as io]))

(defn summary []
    "displays a summary of ongoing processes"
    "") ; keep statistics to display

(defn new-transform [request]
    "process a request for a new transform"
    (info "New Request: " request)
    (validate/request-fields request)
    (let [transform-id (io/get-transform-id request)
        data (io/read-data request)
        transform (io/read-transform request)
        parameters (io/read-parameters request)
        data-namespace (request :DataNamespace)
        transform-name (request :TransformName)]
        (validate/data-definition data)
        (validate/data-compatibility data)
        (validate/transform-definition transform)
        (validate/type-check transform data)
        (validate/transform-parameters transform parameters)
        (io/process-transform transform-id transform-name data-namespace parameters data)
        ; call
        ; write data spec
        ; log results in elastic search
        "Success"))


(defroutes app-routes
  (GET "/summary" [] (summary))
  (POST "/new-transform" [& request] (io/catch-all new-transform request))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
