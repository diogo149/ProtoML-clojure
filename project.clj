(defproject protoml "0.1.0-SNAPSHOT"
  :description "clojure implementation of ProtoML's ideas - for fun!"
  :url "https://github.com/diogo149/ProtoML-clojure"
  :license {:name "Apache License Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [org.clojure/data.json "0.2.3"]
                 [org.clojure/tools.logging "0.2.6"]
                 [compojure "1.1.5"]
                 ; [ring-json-params "0.1.3"]
                 ; [clj-logging-config "1.9.10"] this package messes up logging
                 [me.raynes/fs "1.4.4"]
                 [digest "1.4.3"]]
  :plugins [[lein-ring "0.8.5"]]
  :ring {:handler protoml.handler/app}
  :profiles
  {:dev {:dependencies [[ring-mock "0.1.5"]]}})
