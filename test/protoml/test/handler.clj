(ns protoml.test.handler
  (:use clojure.test
        ring.mock.request
        protoml.handler))

(deftest test-app
  (testing "tutorial route"
    (let [response (app (request :get "/"))]
      (is (= (:status response) 200))
      (is (= (:body response) (tutorial)))))

  (testing "not-found route"
    (let [response (app (request :get "/invalid"))]
      (is (= (:status response) 404)))))
