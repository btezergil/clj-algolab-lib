(ns btezergil.constants)

(def api-hostname "https://www.algolab.com.tr/api")

(def session-refresh-path  "/SessionRefresh")
(def equity-info-path "/GetEquityInfo")
(def candle-path "/GetCandleData")

(def login-endpoint (str api-hostname "/api/LoginUser"))
(def login-sms-endpoint (str api-hostname "/api/LoginUserControl"))
(def session-refresh-endpoint (str api-hostname "/api" session-refresh-path))
(def equity-info-endpoint (str api-hostname "/api" equity-info-path))
(def candle-endpoint (str api-hostname "/api" candle-path))
