(ns sv.timbre.gcloud.logging.metadata
  (:require [clj-http.lite.client :as lc]
            [cheshire.core :as ch]))

(def instance-metadata-sample
  {:zone "projects/123/zones/europe-west1-b"
   :hostname "server-3.c.project-789.internal"
   :id 456})

(defn extract-zone [zone-str]
  (second (re-find #".*/zones/(.*)" zone-str)))

(defn extract-resource-name [hostname]
  (second (re-find #"^(.*?)\." hostname)))

(defn extract-project-id [hostname]
  (second (re-find #".*\.(.*?)\.internal$" hostname)))

(defn prepare-log-metadata [instance-metadata]
  (let [project-id (extract-project-id (:hostname instance-metadata))]
    {:logName (str "projects/"
                   project-id
                   "/logs/clj")
     :resource {:type "gce_instance"
                :labels
                {:zone (extract-zone (:zone instance-metadata))
                 :instance_id (str (:id instance-metadata))}}}))

(def instance-metadata-request
  {:request-method :get
   ;; the explicit IP address of the metadata.google.internal server
   ;; is used to avoid name server resolution issues, especially in
   ;; Docker containers where the metadata.google.internal entry is
   ;; missing in the /etc/hosts
   :url "http://169.254.169.254/computeMetadata/v1/instance/"
   ;; this is also done by Google Cloud API Java client:
   ;; https://github.com/google/google-api-java-client/blob/1.23.0/google-api-client/src/main/java/com/google/api/client/googleapis/auth/oauth2/OAuth2Utils.java#L42
   :query-params {:recursive true}
   :headers {"Metadata-Flavor" "Google"}})

(defn get-entry-metadata*
  "Only works on a Google Compute Engine (GCE) instance (since
   http://metadata.google.internal/ is only available there)"
  [request-fn]
  (let [response (request-fn instance-metadata-request)]
    (prepare-log-metadata (:body response))))

(defn get-entry-metadata []
  (get-entry-metadata*
   (fn [req]
     (update
      (lc/request req)
      :body
      #(ch/parse-string % true)))))
