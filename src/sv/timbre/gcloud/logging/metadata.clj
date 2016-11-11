(ns sv.timbre.gcloud.logging.metadata)

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
  {:project_id (extract-project-id (:hostname instance-metadata))
   :zone (extract-zone (:zone instance-metadata))
   :instance_id (str (:id instance-metadata))})

(def instance-metadata-request
  {:request-method :get
   :url "http://metadata.google.internal/computeMetadata/v1/instance/"
   :query-params {:recursive true}
   :headers {"Metadata-Flavor" "Google"}
   :as :json})

(defn get-log-metadata
  "Only works on a Google Compute Engine (GCE) instance (since
   http://metadata.google.internal/ is only available there)"
  [request-fn]
  (let [response (request-fn instance-metadata-request)]
    (prepare-log-metadata (:body response))))
