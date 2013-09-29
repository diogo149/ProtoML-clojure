(ns protoml.io
  (:use [clojure.contrib.map-utils :only [safe-get]])
  (:require [protoml.shell :as shell]
            [protoml.parse :as parse]
            [protoml.utils :as utils]
            [me.raynes.fs :as fs]))

(comment "This file is meant to be as simple as possible, and the layer in between the filesystem and ProtoML, so that it's easy to switch out.")

(defn path-join [& args]
  "joins path arguments into a single string, returns a string"
  (clojure.string/replace (apply str (interpose "/" args)) #"/+" "/"))

(defn safe-slurp [filename]
  "slurp with errors caught, returns [value error]"
  (utils/exception-to-error slurp filename))

(defn safe-spit [filename string]
  "spit with errors caught, returns [value error]"
  (utils/exception-to-error spit filename string))

(defn safe-open-json [filename]
  "open and parse json, returns [value error]"
  (utils/err->> filename
                safe-slurp
                parse/safe-from-json))

(defn safe-write-json [filename object]
  "convert to json and write to file, returns [value error]"
  (utils/err->> object
                parse/safe-to-json
                (partial safe-spit filename)))

(defn exists? [filename]
  "determine if a file exists, returns a boolean"
  (fs/exists? filename))

(defn create-directory [directory]
  "create a directory and it's parents, returns [value error]"
  (shell/mkdir-p directory))

(defn list-dir [directory]
  (fs/list-dir directory))

(defn make-read-only [filename]
  (shell/make-read-only filename))

(defn check-sh [& arguments]
  "calls input arguments as a shell command, and returns in the form [val error]"
  (apply shell/check-sh arguments))
