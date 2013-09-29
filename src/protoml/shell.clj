(ns protoml.shell
  (:use compojure.core
        [clojure.java.shell :only [sh]]
        [clojure.tools.logging :only (info)])
  (:require [protoml.utils :as utils]))

; for future reference, all arguments must be strings
(defn unsafe-sh [& arguments]
  "calls input arguments as a shell command, and throws an exception if the command fails"
  (info (apply str (concat ["Shell Calling: "] (interpose " " arguments))))
  (let [output (apply sh arguments)
        return-code (output :exit)]
    (info "Shell Output: " output)
    (if (= return-code 0) output
      (throw (Exception. (apply str (concat ["Error calling: "] (interpose " " arguments) [" Stderr: " (output :err)])))))))

(defn check-sh [& arguments]
  "calls input arguments as a shell command, and returns in the form [val error]"
  (apply utils/exception-to-error (conj arguments unsafe-sh)))

(defn mkdir-p [directory-path]
  "makes a directory and it's parents, input should be a directory"
  (check-sh "mkdir" "-p" directory-path))

(defn make-read-only [filename]
  "changes file permissions of input file to be read only"
  (check-sh "chmod" "-R" "-w" "-x" filename))
