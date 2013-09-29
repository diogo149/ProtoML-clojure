(ns protoml.test.validate
  (:use clojure.test
        protoml.validate
        protoml.test.test-utils))

; (deftest test-data-compatibility
;   (testing "always valid for 0 rows"
;     (let [request {:data []}]
;       (= (test-non-error (data-compatibility request)) request)))
;   )

(deftest test-input-directories
  (testing "invalid directories"
    (doall (for [parent-directories [["fake-dir"]
                                     ["." ".." "fake-dir" "/"]]]
             (let [request {:parent-directories parent-directories}]
               (test-error (input-directories request))
               ))))
  (testing "valid input directories"
    (doall (for [parent-directories [[]
                                     ["." ".." "/"]]]
             (let [request {:parent-directories parent-directories}]
               (is (= (test-non-error (input-directories request)) request))
               ))))
  (testing "improper request"
    (doall (for [improper-request [{}
                                   []
                                   ""
                                   3]]
             (test-exception input-directories improper-request)))
    ))
