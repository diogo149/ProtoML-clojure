(ns protoml.test.validate
  (:use clojure.test
        protoml.validate
        protoml.test.test-utils))

(deftest test-data-compatibility
  (testing "always valid for 0 rows"
    (let [request {:data []}]
      (= (test-non-error (data-compatibility request)) request)))
  )
