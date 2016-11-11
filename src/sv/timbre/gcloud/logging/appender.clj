(ns sv.timbre.gcloud.logging.appender
  (:require [sv.gcloud.client :as g]
            [clj-http.lite.client :as lc]
            [clj-time.core :as t]
            [clj-time.format :as tf]
            [taoensso.timbre :as log]
            [cheshire.core :as ch]
            [sv.timbre.gcloud.logging.util.time :as ut]))

(def client-config
  {:scopes ["https://www.googleapis.com/auth/logging.write"]})

(def lite-client
  ;; clj-http.lite is used here, since clj-http creates some debug log
  ;; messages, which would cause an endless loop (since each call of
  ;; the appender would create new log messages)
  (-> lc/request
      (g/wrap-access-token client-config)))

(defn write-log-request [params]
  {:request-method :post
   :url "https://logging.googleapis.com/v2/entries:write"
   :body (ch/generate-string params)
   :content-type "application/json"})

(def level-mapping
  ;; see https://cloud.google.com/logging/docs/api/reference/rest/v2/LogEntry#LogSeverity
  {:debug "DEBUG"
   :info "INFO"
   :warn "WARNING"
   :error "ERROR"
   :fatal "CRITICAL"})

(defn prepare-log-entry-payload [timbre-log-data]
  {:vargs_str (pr-str (:vargs timbre-log-data))
   :msg @(:msg_ timbre-log-data)
   :file (:?file timbre-log-data)
   :hostname @(:hostname_ timbre-log-data)
   :ns_str (:?ns-str timbre-log-data)
   :line (:?line timbre-log-data)})

(defn prepare-log-entry [timbre-log-data]
  ;; see https://cloud.google.com/logging/docs/api/reference/rest/v2/LogEntry
  {:timestamp (ut/unparse-zulu-date-format (:instant timbre-log-data))
   :severity (get level-mapping (:level timbre-log-data) "DEFAULT")
   :insertId (str (.getTime (:instant timbre-log-data)) "_" @(:hash_ timbre-log-data))
   :jsonPayload (prepare-log-entry-payload timbre-log-data)})

(defn appender [context]
  (fn [log-data]
    (when (not= (:context log-data) ::appending)        
      (try
        (log/with-context
          ::appending
          (lite-client
           (write-log-request
            {:entries
             [(merge
               (:log-entry-metadata context)
               (prepare-log-entry log-data))]})))
        (catch Exception e
          ;; ignore since logging could cause an endless loop here, if
          ;; the appending code also writes something to the log (over
          ;; timbre, slf4j etc.).
          )))))
