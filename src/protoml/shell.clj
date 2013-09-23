(comment
    this file is separated out to make it easy to change the underlying layer (to hadoop, for instance)
)

(ns protoml.shell
  (:use compojure.core
            [clojure.java.shell :only [sh]]
            [clojure.tools.logging :only (info error)]))

(defn check-sh [& arguments]
    "calls input arguments as a shell command, and throws an exception if the command fails"
    (apply info (concat ["Shell Calling: "] arguments))
    (let [output (apply sh arguments)
        return-code (output :exit)]
        (info "Shell Output: " output)
        (if (= return-code 0) output
            (throw (Exception. (apply str (concat ["Error calling: "] (interpose " " arguments) [" Stderr: " (output :err)])))))))

(defn mkdir-p [directory-path]
    "makes a directory and it's parents, input should be a directory"
    (check-sh "mkdir" "-p" directory-path))

(defn generic-call [executable input-map]
    "calls an executable, transforming input-map into command line arguments"
    (loop [curr input-map acc [executable]]
        (if (empty? curr) (apply check-sh acc)
            (let [H (first curr)
                T (rest curr)
                param (first H)
                value (second H)
                to-arg (fn [v] (str "--" param "=" v))]
                (if (coll? value) (recur T (concat acc (map to-arg value)))
                    (recur T (conj acc (to-arg value))))))))

(defn call-transform [transform-path parameters input-paths output-paths model-path train-mode random-seed]
    "calls the transform file to perform the transform"
    (let [input-map (merge parameters {:input input-paths :output output-paths :model model-path :trainmode train-mode :seed random-seed})]
        (generic-call transform-path input-map)))

(defn make-read-only [filename]
    "changes file permissions of input file to be read only"
    (check-sh "chmod" "-R" "-w" "-x" filename))
