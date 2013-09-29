(ns protoml.test.pipeline
  (:use clojure.test
        protoml.pipeline
        protoml.test.test-utils)
  )

(def md5-1 "99914b932bd37a50b983c5e7c90ae93b")
(def md5-2 "3fba38625fe2c5345bb68671cea9d91e")
(def md5-3 "e1105186c6ef6b7aaa4af46a71973f26")
(def md5s [md5-1 md5-2 md5-3])

(deftest test-append-transform-id
  (testing "md5 hashes"
    (is (= (append-transform-id {})
           [{:transform-id md5-1} nil]))
    (is (= (append-transform-id {:doo "doo"})
           [{:doo "doo" :transform-id md5-2} nil]))
    ; be careful with flatten, since that may remove empty collections
    (is (= (append-transform-id {:doo []})
           [{:doo [] :transform-id md5-3} nil]))
    ))


(deftest test-append-output-extensions
  (testing "valid input"
    (doall
      (for [[output output-extensions] [[[] []]
                                        [[{:Extension "vw"}] ["vw"]]
                                        [[{:Extension "vw"} {:Extension "csv"}] ["vw" "csv"]]]]
        (let [request {:transform {:Output output}}
              desired-output (assoc request :output-extensions output-extensions)]
          (is (= (test-non-error (append-output-extensions request))
                 desired-output)))))
    ))
