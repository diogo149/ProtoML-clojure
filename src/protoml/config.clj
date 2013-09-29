(ns protoml.config
  (:require [protoml.parse :as parse]
            [protoml.io :as io]))

(def data-extension ".pml-data")
(def model-extension ".pml-model")
(def transform-extension ".json")
(def CONFIG-FILE "ProtoML_config.json")
(def CONFIG (try (parse/from-json (slurp CONFIG-FILE)) (catch Throwable e {}))) ; defaults to empty map if file is not found
(def train-namespace (CONFIG :TrainNamespace "train"))
(def root-directory (CONFIG :RootDir "."))
(def hadoop-mode (CONFIG :HadoopMode false))
(def directory-depth (CONFIG :DirectoryDepth 5))
(def chars-per-directory-level (CONFIG :CharsPerDirectoryLevel 2))
(def data-folder (io/path-join root-directory "data"))
(def transform-folder (io/path-join root-directory "transform"))
