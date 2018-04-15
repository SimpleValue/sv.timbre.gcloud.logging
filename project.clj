(defproject sv/timbre.gcloud.logging "0.1.4"
  :description "A timbre appender for the Google Cloud Logging service"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.taoensso/timbre "4.7.4"]

                 [clj-http-lite "0.3.0"]
                 [cheshire "5.6.3"]
                 [sv/gcloud.client "0.1.3"]
                 [clj-time "0.12.2"]])
