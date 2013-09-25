(ns protoml.test.io
  (:use clojure.test
        protoml.io))

(deftest test-generate-transform-id
  (testing "md5 hashes"
    (is (= (generate-transform-id {})
           [{:transform-id "99914b932bd37a50b983c5e7c90ae93b"} nil]))
    (is (= (generate-transform-id {:doo "doo"})
           [{:doo "doo" :transform-id "3fba38625fe2c5345bb68671cea9d91e"} nil]))
    ; be careful with flatten, since that may remove empty collections
    (is (= (generate-transform-id {:doo []})
           [{:doo [] :transform-id "e1105186c6ef6b7aaa4af46a71973f26"} nil]))
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

