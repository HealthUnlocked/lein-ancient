(ns ancient-clj.io
  (:require [ancient-clj.io
             [http :refer [http-loader]]
             [local :refer [local-loader]]
             [s3 :refer [s3-loader]]]
            [clojure.java.io :as io])
  (:import [java.net URL URISyntaxException]))

;; ## Loader Creation

(defmulti ^:private loader-for*
  (fn [{:keys [uri]}]
    (when (string? uri)
      (-> ^String uri
          (.split ":/" 2)
          (first)
          (keyword))))
  :default nil)

(defn loader-for
  [v]
  (cond (fn? v) v
        (string? v) (loader-for* {:uri v})
        (and (map? v) (string? (:uri v))) (loader-for* v)
        :else (throw
                (Exception.
                  (format "cannot create loader from: %s" (pr-str v))))))

;; ## Loaders

(defmethod loader-for* :http
  [{:keys [uri] :as opts}]
  (http-loader uri opts))

(defmethod loader-for* :https
  [{:keys [uri] :as opts}]
  (http-loader uri opts))

(defmethod loader-for* :file
  [{:keys [uri] :as opts}]
  (let [url (URL. uri)]
    (-> (try
          (io/file (.toURI url))
          (catch URISyntaxException _
            (io/file (.getPath url))))
        (local-loader opts))))

(defmethod loader-for* :s3p
  [{:keys [uri] :as opts}]
  {:pre [(.startsWith ^String uri "s3p://")]}
  (let [[bucket path] (-> (subs uri 6)
                          (.split "/" 2))]
    (assert (not (empty? bucket)))
    (assert (not (empty? path)))
    (s3-loader bucket (assoc opts :path path))))
