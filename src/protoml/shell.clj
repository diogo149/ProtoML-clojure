(comment
    this file is separated out to make it easy to change the underlying layer (to hadoop, for instance)
)

(ns protoml.io
  (:use compojure.core
            [clojure.java.shell :only [sh]]
            [clojure.tools.logging :only (info error)]))

(defn mkdir-p [directory-path]
    "makes a directory and it's parents, input should be a directory"
    (info "Calling mkdir-p: directory-path")
    (sh "mkdir" "-p" directory-path))

(defn call-transform [transform-path parameters input-paths output-paths model-path train-mode random-seed]
    "calls the transform file to perform the transform"
    (let [input-map (merge parameters {:input input-paths :output output-paths :model model-path :trainmode train-mode :seed random-seed})]
        (generic-call transform-path input-map)))

(defn generic-call [executable input-map]
    "calls an executable, transforming input-map into command line arguments"
    (loop [curr input-map acc [executable]]
        (if (empty? curr) (apply sh acc)
            (let [H (first curr)
                T (rest curr)
                param (first H)
                value (second H)
                to-arg (fn [v] (str "--" param "=" v))]
                (if (coll? value) (recur T (concat acc (map to-arg value)))
                    (recur T (conj acc (to-arg value))))))))
