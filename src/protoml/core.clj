(ns protoml.core
  (:use [digest :only (md5)])
  (:require [protoml.io :as io]
            [protoml.config :as config]
            [me.raynes.fs :as fs]))


(defn determine-transform-id [request]
  "determine transform id from hashing input request"
  (->> request
       vec
       (apply concat)
       (apply sorted-map)
       str
       md5))

(defn determine-parent-directory [transform-id]
  "determine the directory for a specific transform"
  (let [partitioned (partition config/chars-per-directory-level transform-id)
        subset (take config/directory-depth partitioned)
        directory-list (map (partial apply str) subset)
        directory (apply io/path-join (concat [config/data-folder] directory-list [transform-id]))]
    directory))

(defn extract-parent-id [datum-id]
  "recovers the id of the parent transform and the index of datum"
  (rest (re-find #"^([0-9a-f]{32})--(\d+)$" datum-id)))

(defn to-datum-id [parent-id index]
  "creates a datum id from a parent id and index"
  (str parent-id "--" index))

(defn train-mode? [data-namespace]
  "determines whether or not a namespace is for training"
  (= config/train-namespace data-namespace))

(defn to-output-prefixes [directory transform-id output-num]
  "creates the prefixes for the output of a transform"
  (for [i (range 1 (inc output-num))]
    (io/path-join directory (to-datum-id transform-id i))))

(defn extract-data-extensions [directory data-id]
  "returns a collection of extensions available for a certain data-id"
  (let [files (io/list-dir directory)]
    (if (nil? files) (throw (Exception. "Invalid directory to extract data extensions from"))
      (let [extensions (map fs/extension files)
            non-nil (filter (complement nil?) extensions)
            data-extensions (filter #(not= % config/data-extension) non-nil)]
        data-extensions))))

(defn generate-random-seed [transform-id]
  "generate a random seed"
  (Math/abs (hash transform-id)))

(defn generic-call [executable input-map]
  "calls an executable, transforming input-map into command line arguments"
  (loop [curr input-map acc [executable]]
    (if (empty? curr) (apply io/check-sh acc)
      (let [H (first curr)
            T (rest curr)
            param (first H)
            value (second H)
            to-arg (fn [v] (str "--" (name param) "=" v))]
        (if (coll? value) (recur T (concat acc (map to-arg value)))
          (recur T (concat acc [(to-arg value)])))))))
