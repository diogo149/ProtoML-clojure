(ns protoml.test.io
  (:use clojure.test
        protoml.io
        protoml.test.test-utils)
  (:require [clj-logging-config.log4j :as log-config]))

; silence logging
; (log-config/set-logger! :pattern "")

(def md5-1 "99914b932bd37a50b983c5e7c90ae93b")
(def md5-2 "3fba38625fe2c5345bb68671cea9d91e")
(def md5-3 "e1105186c6ef6b7aaa4af46a71973f26")

(def md5s [md5-1 md5-2 md5-3])

; TODO create sample requests to use

(deftest test-path-join
  (testing "samples"
    (is (= (path-join "doo/" "/poo") "doo/poo"))
    (is (= (path-join "/doo/" "/poo") "/doo/poo"))
    (is (= (path-join "./doo/" "/poo") "./doo/poo"))
    (is (= (path-join "./doo/" "/poo/" "/foo/" "/goo/") "./doo/poo/foo/goo/"))

    ))

(deftest test-generate-transform-id
  (testing "md5 hashes"
    (is (= (generate-transform-id {})
           [{:transform-id md5-1} nil]))
    (is (= (generate-transform-id {:doo "doo"})
           [{:doo "doo" :transform-id md5-2} nil]))
    ; be careful with flatten, since that may remove empty collections
    (is (= (generate-transform-id {:doo []})
           [{:doo [] :transform-id md5-3} nil]))
    ))

(deftest test-extract-parent-id
  (testing "proper data ids"
    (is (= (extract-parent-id "3fba38625fe2c5345bb68671cea9d91e--3") ["3fba38625fe2c5345bb68671cea9d91e" "3"]))
    (is (= (extract-parent-id "99914b932bd37a50b983c5e7c90ae93b--921913") ["99914b932bd37a50b983c5e7c90ae93b" "921913"]))
    )
  (testing "improper data ids"
    ; letters in index
    (is (= (extract-parent-id "99914b932bd37a50b983c5e7c90ae93b--a921913") []))
    ; decimal point in index
    (is (= (extract-parent-id "99914b932bd37a50b983c5e7c90ae93b--921913.4") []))
    ; forward slash in parent id
    (is (= (extract-parent-id "99914b932bd37a50b983c5e7c90ae9/b--921913") []))
    ; capital letter in parent id
    (is (= (extract-parent-id "99914b932bd37a50b983c5e7c90ae93B--921913") []))
    ; over 32 character parent id
    (is (= (extract-parent-id "99914b932bd37a50b983c5e7c90ae93ba--921913") []))
    ; under 32 character parent id
    (is (= (extract-parent-id "d00--1") []))
    ))

(deftest test-to-datum-id
  (testing "with input from extract-parent-id"
    (doall
      (for [md5-x md5s index (map str (range 10))]
        (is (= (extract-parent-id (to-datum-id md5-x index)) [md5-x index]))))))

(deftest test-generate-data-parent-ids
  (testing "proper data ids"
    ; TODO
    )
  (testing "improper data ids"
    ; TODO
    ))

(deftest test-safe-open-json
  (testing "invalid file"
    (test-error (safe-open-json "fake-filename"))
    )
  (testing "valid file"
    ; TODO
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

(deftest test-generate-output-extensions
  (testing "valid input"
    (doall
      (for [[output output-extensions] [[[] []]
                                        [[{:extension "vw"}] ["vw"]]
                                        [[{:extension "vw"} {:extension "csv"}] ["vw" "csv"]]]]
        (let [request {:transform {:Output output}}
              desired-output (assoc request :output-extensions output-extensions)]
          (is (= (test-non-error (generate-output-extensions request))
                 desired-output)))))
    ))
