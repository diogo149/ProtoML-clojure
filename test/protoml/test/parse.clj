(ns protoml.test.parse
  (:use clojure.test
        protoml.parse
        protoml.test.test-utils))

(deftest test-safe-to-json
  (testing "correct input"
    (is (= (safe-to-json {}) ["{}" nil]))
    (is (= (safe-to-json (sorted-map :a 1)) ["{\"a\":1}" nil]))
    (is (= (safe-to-json (sorted-map :a 1 :b "2")) ["{\"a\":1,\"b\":\"2\"}" nil]))
    (is (= (safe-to-json (sorted-map :a 1
                                     :b "2"
                                     :c [{}, "doo"]))
           ["{\"a\":1,\"b\":\"2\",\"c\":[{},\"doo\"]}" nil]))
    )
  (testing "incorrect input"
    (test-error (safe-to-json (Exception.)))
    (test-error (safe-to-json (java.util.Date.)))
    ))
