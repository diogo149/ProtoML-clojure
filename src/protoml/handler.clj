(ns protoml.handler
  (:use compojure.core)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [protoml.validate :as validate]
            [protoml.parse :as parse]
            [protoml.io :as io]
            [protoml.parse :as parse]
            [protoml.utils :as utils]
            [protoml.pipeline :as pipeline]
            [protoml.elastic-search :as es]
            [protoml.new-transform]
            [protoml.manual-input]))

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
          pipeline/parse-request
          validate/request-types
          validate/output-definitions
          ; transform
          pipeline/read-transform
          validate/transform-definition
          pipeline/append-transform-path
          validate/transform-path
          ; input data
          pipeline/append-parent-ids
          pipeline/append-parent-directories
          validate/input-directories
          pipeline/append-input-prefixes
          pipeline/append-input-definitions
          validate/input-definitions
          pipeline/append-input-extensions
          pipeline/append-input-paths
          validate/input-paths
          pipeline/read-data
          validate/data-definition
          ; transform parameters
          pipeline/append-parameters
          validate/transform-parameters
          ; data-transform compatibility
          validate/type-check
          ; output
          pipeline/append-transform-id
          pipeline/append-directory
          pipeline/append-output-num
          pipeline/append-output-prefixes
          pipeline/append-output-extensions
          pipeline/append-output-paths
          pipeline/append-output-definitions
          pipeline/append-output-definitions-content
          ; TODO validate/output-definitions-content
          ; misc parameters
          pipeline/append-random-seed
          validate/random-seed
          pipeline/append-model-path
          pipeline/append-train-mode
          ; create directory: this should be last so it isn't created if the request fails earlier
          pipeline/create-directory
          ; performing the operation
          validate/no-nil
          validate/new-transform-types
          protoml.new-transform/process-transform
          validate/output-paths
          ; post-processing
          pipeline/make-model-immutable
          pipeline/make-output-immutable
          pipeline/write-output-definitions
          pipeline/make-output-definitions-immutable
          validate/output-definitions
          )
        output-map (if (nil? error) result
                     (assoc request :error error))]
    (when (nil? error)
      (es/create-new-transform output-map)) ; this causes errors when there's a hash collision (?)
    (parse/to-json output-map)
    ))

(defn manual-input [request]
  "manually add new data"
  (let [[result error]
        (utils/log-err->>
          request
          ; request pre-processing
          validate/manual-request-fields
          pipeline/parse-request
          ; TODO validation/manual-request-types
          ; TODO validation/data-type
          ; TODO validation/filepath
          protoml.manual-input/manual-determine-extension
          ; output
          pipeline/append-transform-id
          pipeline/append-directory
          (partial pipeline/add-key :output-num 1)
          pipeline/append-output-prefixes
          protoml.manual-input/manual-output-extension
          pipeline/append-output-paths
          pipeline/append-output-definitions
          protoml.manual-input/manual-output-definitions-content
          ; TODO validate/output-definitions-content
          ; create directory: this should be last so it isn't created if the request fails earlier
          pipeline/create-directory
          ; perform the operation
          validate/no-nil
          ; TODO validate/manual-input-types
          protoml.manual-input/process-manual-input
          validate/output-paths
          ; post-processing
          pipeline/make-output-immutable
          pipeline/write-output-definitions
          pipeline/make-output-definitions-immutable
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
