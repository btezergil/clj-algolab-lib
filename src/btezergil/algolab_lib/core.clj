(ns btezergil.algolab-lib.core
  (:require [clojure.string :as str]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [envvar.core :as envvar :refer [env]]
            [clj-http.client :as client]
            [btezergil.algolab-lib.constants :as c])
  (:import (javax.crypto Cipher)
           (javax.crypto.spec SecretKeySpec)
           (java.security MessageDigest)
           (org.apache.commons.codec.binary Base64 Hex)))

(def apikey (:algolab-apikey @env))
(def username (:algolab-username @env))
(def password (:algolab-password @env))

(def aes-key (second (str/split apikey #"-")))
(def token (atom nil))
(def checker-hash (atom nil))

(defn encrypt
  "Encryption function for login flow, uses AES encryption as required by ALGOLAB."
  [text]
  (let  [text-bytes (.getBytes text "UTF-8")
         key-spec (SecretKeySpec. (Base64/decodeBase64 (.getBytes aes-key "UTF-8")) "AES")
         iv (byte-array 16)
         cipher (Cipher/getInstance "AES/CBC/PKCS5Padding")]
    (.init cipher Cipher/ENCRYPT_MODE key-spec (javax.crypto.spec.IvParameterSpec. iv))
    (Base64/encodeBase64String (.doFinal cipher text-bytes))))

(defn generate-checker
  "Generates the checker that is used to validate every request."
  [endpoint payload]
  (let [data (str apikey c/api-hostname endpoint payload)
        digest (MessageDigest/getInstance "SHA256")]
    (Hex/encodeHexString (.digest digest (.getBytes data "UTF-8")))))

(defn set-checker-hash-externally
  "Sets the checker hash externally to use an already existing session."
  [cur-hash]
  (reset! checker-hash cur-hash))

(defn login
  "Initializes login flow by sending an SMS to the phone number of the account that is firing the request.
  Use the SMS received for getting the hash in login-sms-code function."
  []
  (let [encrypted-username (encrypt username)
        encrypted-passwd (encrypt password)
        payload (json/write-str {:Username encrypted-username
                                 :Password encrypted-passwd})
        response (client/post c/login-endpoint
                              {:content-type :json
                               :body payload
                               :headers {"APIKEY" apikey}})
        body (json/read-str (:body response) :key-fn keyword)]
    (if (:success body)
      (do (reset! token (-> body :content :token))
          (log/info "Login to ALGOLAB succeeded, token received, please insert SMS code next."))
      (do (log/error "Login to ALGOLAB failed, no token received. Response: " response)
          (throw (Exception. "Login to ALGOLAB failed."))))))

(defn login-sms-code
  "Uses the SMS code received in the first part of the login flow to fetch the hash that is used to generate the checker."
  [sms-code]
  (let [encrypted-token (encrypt @token)
        encrypted-passwd (encrypt sms-code)
        payload (json/write-str {:token encrypted-token
                                 :Password encrypted-passwd})
        response (client/post c/login-sms-endpoint
                              {:content-type :json
                               :body payload
                               :headers {"APIKEY" apikey}})
        body (json/read-str (:body response) :key-fn keyword)]
    (if (:success body)
      (do (reset! checker-hash (-> body :content :hash))
          (log/info "Login to ALGOLAB succeeded, hash received.")
          @checker-hash)
      (do (log/error "Login to ALGOLAB failed, no hash received. Response: " response)
          (throw (Exception. "Login to ALGOLAB failed during SMS verification."))))))

(defn session-refresh
  "Refreshes the existing session."
  []
  {:pre [(-> @checker-hash nil? not)]}
  (try (let [checker (generate-checker c/session-refresh-path "")
             response (client/post c/session-refresh-endpoint
                                   {:content-type :json
                                    :body (json/write-str {})
                                    :headers {"APIKEY" apikey
                                              "Checker" checker
                                              "Authorization" @checker-hash}})]
         (when (= 200 (:status response))
           (log/info "Session refreshed successfully.")))
       (catch Exception e
         (do (log/error "Session failed to refresh. Exception: " e)
             (throw e)))))

(defn equity-info
  [equity]
  {:pre [(-> @checker-hash nil? not)]}
  (try (let [payload (json/write-str {:symbol equity})
             checker (generate-checker c/equity-info-path payload)
             response (client/post c/equity-info-endpoint
                                   {:content-type :json
                                    :body payload
                                    :headers {"APIKEY" apikey
                                              "Checker" checker
                                              "Authorization" @checker-hash}})
             body (json/read-str (:body response) :key-fn keyword)]
         body)
       (catch Exception e
         (do (log/error "Failed to get equity info. Exception: " e)
             (throw e)))))

(defn candle-data
  "Fetches the OHLC data for the given equity. 
  Optional period parameter determines the candle period in minutes.
  250 candles returned for 1 hour."
  [equity & {:keys [period]
             :or {period "60"}}]
  {:pre [(-> @checker-hash nil? not)]}
  (try (let [payload (json/write-str {:symbol equity
                                      :period period})
             checker (generate-checker c/candle-path payload)
             response (client/post c/candle-endpoint
                                   {:content-type :json
                                    :body payload
                                    :headers {"APIKEY" apikey
                                              "Checker" checker
                                              "Authorization" @checker-hash}})
             body (json/read-str (:body response) :key-fn keyword)
             candles (:content body)]
         (when (:success body)
           candles))
       (catch Exception e
         (do (log/error "Failed to get candle data. Exception: " e)
             (throw e)))))
