(ns protoml.utils
  (:use compojure.core))

(comment this stuff is unused and can be moved/removed)

(defmacro get-var-name [x]
  `(:name (meta (var ~x))))

(defmacro name-and-value [x]
  `(str (:name (meta (var ~x))) ": " ~x))
