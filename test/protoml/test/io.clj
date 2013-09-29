(ns protoml.test.io
  (:use clojure.test
        protoml.io
        protoml.test.test-utils)
  )

(deftest test-path-join
  (testing "samples"
    (is (= (path-join "doo/" "/poo") "doo/poo"))
    (is (= (path-join "/doo/" "/poo") "/doo/poo"))
    (is (= (path-join "./doo/" "/poo") "./doo/poo"))
    (is (= (path-join "./doo/" "/poo/" "/foo/" "/goo/") "./doo/poo/foo/goo/"))
    ))

(deftest test-safe-open-json
  (testing "invalid file"
    (test-error (safe-open-json "fake-filename"))
    )
  (testing "valid file"
    (test-non-error (safe-open-json "ProtoML_config.json"))
    ))

(deftest test-safe-slurp
  (testing "invalid file"
    (test-error (safe-slurp "fake-filename"))
    (test-error (safe-slurp {}))
    (test-error (safe-slurp []))
    )
  (testing "valid file"
    (is (= (test-non-error (safe-slurp "README.md"))
           (slurp "README.md")))
    ))

