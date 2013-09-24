(ns protoml.test.utils
  (:use clojure.test
        protoml.utils))

(def error-text "this is an error")

(defn error-func [_] [nil error-text])

(defn successful-func [x] [x nil])

(deftest test-log-err->>
    (testing "successful functions"
        (is (= [3 nil] (log-err->> 3 successful-func)))
        (is (= [4 nil] (log-err->> 4 successful-func successful-func)))
        (is (= [5 nil] (log-err->> 5 successful-func successful-func successful-func)))
        ))

(deftest test-log-err->>
    (testing "error returning functions"
        (is (= [nil error-text] (log-err->> 1 error-func)))
        (is (= [nil error-text] (log-err->> 2 error-func error-func)))
        (is (= [nil error-text] (log-err->> 3 error-func successful-func)))
        (is (= [nil error-text] (log-err->> 4 successful-func error-func)))
        (is (= [nil error-text] (log-err->> 5 successful-func successful-func successful-func error-func)))
        ))

