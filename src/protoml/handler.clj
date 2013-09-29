(ns protoml.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [protoml.validate :as validate]
            [protoml.parse :as parse]
            [protoml.io :as io]
            [protoml.parse :as parse]
            [protoml.utils :as utils]))

(defn tutorial []
  "display a tutorial of how to use ProtoML"
  "TODO: tutorial")

(defn summary []
  "displays a summary of ongoing processes"
  "TODO: keep statistics to display")

(defn new-transform [request]
  "process a request for a new transform"
  (let [[result error]
        (utils/log-err->>
          request
          ; request pre-processing
          validate/request-fields
          parse/parse-request
          validate/request-types
          validate/output-definitions
          ; transform
          io/read-transform
          validate/transform-definition
          io/generate-transform-path
          validate/transform-path
          ; input data
          io/generate-data-parent-ids
          io/generate-parent-directories
          validate/input-directories
          io/generate-input-prefixes
          io/generate-input-definitions
          validate/input-definitions
          io/generate-input-extensions
          io/generate-input-paths
          validate/input-paths
          io/read-data
          validate/data-definition
          ; transform parameters
          io/read-parameters
          validate/transform-parameters
          ; data-transform compatibility
          validate/type-check
          ; output
          io/generate-transform-id
          io/generate-directory
          io/generate-output-num
          io/generate-output-prefixes
          io/generate-output-extensions
          utils/debug-request
          io/generate-output-paths
          io/generate-output-definitions
          io/generate-output-definitions-content
          ; TODO validate/output-definitions-content
          ; misc parameters
          io/read-random-seed
          validate/random-seed
          io/generate-model-path
          io/generate-train-mode
          ; create directory: this should be last so it isn't created if the request fails earlier
          io/create-directory
          ; performing the operation
          validate/no-nil
          validate/new-transform-types
          io/process-transform
          validate/output-paths
          ; post-processing
          io/make-model-immutable
          io/make-output-immutable
          io/write-output-definitions
          ; TODO io/make-output-definitions-immutable
          validate/output-definitions
          )
        output-map (if (nil? error) result
                     (assoc request :error error))]
    ; TODO log in elastic search
    ; (Thread/sleep 20000)
    (parse/to-json output-map)
    ))

(defn manual-input [request]
  "manually add new data"
  (let [[result error]
        (utils/log-err->>
          request
          ; request pre-processing
          validate/manual-request-fields
          parse/parse-request
          ; TODO validation/manual-request-types
          ; TODO validation/data-type
          ; TODO validation/filepath
          io/manual-generate-extension
          ; output
          io/generate-transform-id
          io/generate-directory
          (partial io/set-output-num 1)
          io/generate-output-prefixes
          io/manual-output-extension
          io/generate-output-paths
          io/generate-output-definitions
          io/manual-output-definitions-content
          ; TODO validate/output-definitions-content
          ; create directory: this should be last so it isn't created if the request fails earlier
          io/create-directory
          ; perform the operation
          validate/no-nil
          ; TODO validate/manual-input-types
          io/process-manual-input
          validate/output-paths
          ; post-processing
          io/make-output-immutable
          io/write-output-definitions
          ; TODO io/make-output-definitions-immutable
          validate/output-definitions
          )
        output-map (if (nil? error) result
                     (assoc request :error error))]
    ; TODO log in elastic search
    (parse/to-json output-map)
    ))

(defroutes app-routes
  (GET "/" [] (tutorial))
  (GET "/summary" [] (summary))
  (POST "/new-transform" [& request] (new-transform request))
  (POST "/manual-input" [& request] (manual-input request))
  (POST "/echo" [& request] (println "request: " request) (str request))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
