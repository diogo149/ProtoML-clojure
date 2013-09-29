(ns protoml.manual-input
  (:use [clojure.contrib.map-utils :only [safe-get]])
  (:require [me.raynes.fs :as fs]
            [protoml.utils :as utils]))

(defn manual-output-extension [request]
  "set the output extension for manual input"
  (let [extension (safe-get request :extension)
        output-extensions [extension]]
    [(assoc request :output-extensions output-extensions) nil]))

(defn manual-determine-extension [request]
  "parse the extension from the given filepath"
  (let [filepath (safe-get request :Filepath)
        extension (fs/extension filepath)]
    (if (nil? extension) [nil "Filepath has no extension"]
      [(assoc request :extension extension) nil])))

(defn manual-output-definitions-content [request]
  "set the output-definitions-content from the input request"
  (let [NCols (safe-get request :NCols)
        Extension (safe-get request :extension)
        Type (safe-get request :Type)
        output-definitions-content [{:Type Type :Ncols NCols :Extension Extension}]]
    [(assoc request :output-definitions-content output-definitions-content) nil]))

(defn process-manual-input [request]
  "process the manual input by copying the file at the given filepath to the proper location in the data folder"
  (let [filepath (safe-get request :Filepath)
        [output-path] (safe-get request :output-paths)
        [_ error] (utils/exception-to-error fs/copy filepath output-path)]
    (if (nil? error) [request error]
      [nil error])))
