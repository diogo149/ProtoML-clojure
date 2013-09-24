(ns protoml.test.handler
  (:use clojure.test
        ring.mock.request
        protoml.handler))

(deftest test-app
  (testing "main route"
    (let [response (app (request :get "/"))]
      (is (= (:status response) 404))
      (is (= (:body response) "Not Found"))))

  (testing "not-found route"
    (let [response (app (request :get "/invalid"))]
      (is (= (:status response) 404)))))
