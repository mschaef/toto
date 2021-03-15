(ns toto.core.web
  (:gen-class :main true)
  (:use toto.core.util
        compojure.core
        [ring.middleware resource
         not-modified
         content-type
         browser-caching])
  (:require [clojure.tools.logging :as log]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.reload :as ring-reload]
            [ring.middleware.file-info :as ring-file-info]
            [ring.middleware.resource :as ring-resource]
            [ring.util.response :as ring-responsed]
            [compojure.handler :as handler]
            [toto.data.data :as data]
            [toto.view.common :as view-common]
            [toto.view.user :as user]))

(defn add-shutdown-hook [ shutdown-fn ]
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. (fn []
                               (shutdown-fn)))))

(defn wrap-request-logging [ app development-mode? ]
  (fn [req]
    (if development-mode?
      (log/debug 'REQUEST (:request-method req) (:uri req) (:params req))
      (log/debug 'REQUEST (:request-method req) (:uri req)))

    (let [resp (app req)]
      (log/trace 'RESPONSE (:status resp))
      resp)))

(defn wrap-show-response [ app label ]
  (fn [req]
    (let [resp (app req)]
      (log/trace label (dissoc resp :body))
      resp)))

(defn extend-session-duration [ app duration-in-hours ]
  (fn [req]
    (assoc (app req) :session-cookie-attrs {:max-age (* duration-in-hours 3600)})))

(defn- wrap-dev-support [ handler dev-mode ]
  (cond-> (-> handler
              (wrap-request-logging dev-mode)
              (view-common/wrap-dev-mode dev-mode))
    dev-mode (ring-reload/wrap-reload)))

(defn handler [ routes config db-conn ]
  (-> routes
      (wrap-content-type)
      (wrap-browser-caching {"text/javascript" 360000
                             "text/css" 360000})
      (user/wrap-authenticate)
      (data/wrap-db-connection db-conn)
      (extend-session-duration 168)
      (view-common/wrap-remember-query)
      (wrap-dev-support (:development-mode config))
      (handler/site)))

(defn start-site [ routes config db-conn ]
  (let [ { http-port :http-port } config ]
    (log/info "Starting Webserver on port" http-port)
    (let [server (jetty/run-jetty (handler routes config db-conn) { :port http-port :join? false })]
      (add-shutdown-hook
       (fn []
         (log/info "Shutting down webserver")
         (.stop server)))
      (.join server))))

