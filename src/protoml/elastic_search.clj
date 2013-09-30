(ns protoml.elastic-search
  (:use [clojure.tools.logging :only (info)])
  (:require [protoml.config :as config]
            [clojurewerkz.elastisch.rest          :as esr]
            [clojurewerkz.elastisch.rest.document :as esd]
            [clojurewerkz.elastisch.query         :as q]
            [clojurewerkz.elastisch.rest.response :as esrsp]
            [clojure.pprint :as pp]))

; http://clojureelasticsearch.info/articles/getting_started.html

(defn create-new-transform [document]
  "create a document in elastic search"
  (esr/connect! config/elastic-search-url)
  (info "Input protoml.elastic-search/create-new-transform: " document)
  (let [result (esd/create "protoml" "new-transform" document)]
    (info "Output protoml.elastic-search/create-new-transform: " result)
    result))
