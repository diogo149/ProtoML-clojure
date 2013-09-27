(ns protoml.test.test-utils
  (:use clojure.test))

(defn test-error [input]
  "tests that input has nil value and non-empty error string"
  (let [[value error] input]
    (is (= (count input) 2))
    (is (nil? value))
    (is (= (class error) java.lang.String))
    (is (not= error ""))
    error))

(defn test-non-error [input]
  "returns a value and tests that the error is nil"
  (let [[value error] input]
    (is (= (count input) 2))
    (is (nil? error))
    value))
