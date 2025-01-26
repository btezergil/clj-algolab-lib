(ns btezergil.algolab-lib-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.data.json :as json]
            [btezergil.algolab-lib :refer :all]
            [btezergil.constants :refer :all]
            [clj-http.fake :refer [with-fake-routes]]))

(deftest encrypt-test
  (testing "Encrypting username"
    (is (= (encrypt "test username") "gJoGBzmw6Yat4pXvn6KOkw=="))
    (is (= (encrypt "12341324567") "coxb5nvXs7eudFdB1Yuudg==")))
  (testing "Encrypting password"
    (is (= (encrypt "123456") "s+2x8WmPgDjL85SMpXWtvA=="))
    (is (= (encrypt "654321") "NkP0yJF83EuzNWNn/l8T5w=="))))

(deftest checker-test
  (testing "Checker matches with URL:"
    (is (= (generate-checker session-refresh-path "") "253ae32920217976e38ad985c52f94c766689de5a7fd7d09a832060b1199a6db"))
    (is (= (generate-checker equity-info-path (json/write-str {:symbol "TEST"})) "1be1be2d990d4391af473a6918d53f947feb7db7624f422e1c5c069e0604d8c9")))
  (testing "Checker does not match when URL changes:"
    (is (not= (generate-checker "/NotMatchingUrl" (json/write-str {:symbol "TEST"})) "1be1be2d990d4391af473a6918d53f947feb7db7624f422e1c5c069e0604d8c9"))))

(deftest login-test
  (testing "Login successful"
    (with-fake-routes
      {login-endpoint
       {:post (fn [request] {:status 200 :headers {} :body (json/write-str {:success true :content {:token "test-token"}})})}}
      (is (= (login) nil))))
  (testing "Login failed"
    (with-fake-routes
      {login-endpoint
       {:post (fn [request] {:status 200 :headers {} :body (json/write-str {:success false :content nil})})}}
      (is (thrown-with-msg? Exception #"Login to ALGOLAB failed." (login))))))

(deftest login-sms-code-test
  (with-redefs [token (atom "test-token")]
    (testing "Login with SMS code successful"
      (with-fake-routes
        {login-sms-endpoint
         {:post (fn [request] {:status 200 :headers {} :body (json/write-str {:success true :content {:hash "test-hash"}})})}}
        (is (= (login-sms-code "123456") "test-hash"))))
    (testing "Login with SMS code failed"
      (with-fake-routes
        {login-sms-endpoint
         {:post (fn [request] {:status 200 :headers {} :body (json/write-str {:success false :content nil})})}}
        (is (thrown-with-msg? Exception #"Login to ALGOLAB failed during SMS verification." (login-sms-code "123456")))))))

(deftest session-refresh-test
  (with-redefs [checker-hash (atom "test-hash")]
    (testing "Session refresh successful"
      (with-fake-routes
        {session-refresh-endpoint
         {:post (fn [request] {:status 200 :headers {} :body (json/write-str {:success true :content {:hash "test-hash"}})})}}
        (is (= (session-refresh) nil))))
    (testing "Session refresh failed"
      (with-fake-routes
        {session-refresh-endpoint
         {:post (fn [request] {:status 401 :headers {} :body nil})}}
        (is (thrown? Exception  (session-refresh)))))))

(deftest equity-info-test
  (with-redefs [checker-hash (atom "test-hash")]
    (testing "Get equity info call successful"
      (with-fake-routes
        {equity-info-endpoint
         {:post (fn [request] {:status 200 :headers {} :body (json/write-str {:success true :content {:open 1 :high 4 :low 1 :close 3}})})}}
        (is (= (equity-info "SAHOL") {:success true :content {:open 1 :high 4 :low 1 :close 3}}))))
    (testing "Get equity info call failed"
      (with-fake-routes
        {equity-info-endpoint
         {:post (fn [request] {:status 401 :headers {} :body nil})}}
        (is (thrown? Exception  (equity-info "SAHOL")))))))

(deftest candle-test
  (with-redefs [checker-hash (atom "test-hash")]
    (testing "Get candles call successful"
      (with-fake-routes
        {candle-endpoint
         {:post (fn [request] {:status 200 :headers {} :body (json/write-str {:success true :content [{:open 1 :high 4 :low 1 :close 3}]})})}}
        (is (= (candle-data "SAHOL") [{:open 1 :high 4 :low 1 :close 3}]))))
    (testing "Get candles call failed"
      (with-fake-routes
        {candle-endpoint
         {:post (fn [request] {:status 401 :headers {} :body nil})}}
        (is (thrown? Exception  (candle-data "SAHOL")))))))
