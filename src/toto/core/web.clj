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
            [toto.core.session :as store]
            [toto.core.data :as data]
            [toto.view.common :as view-common]
            [toto.view.user :as user]))

(defn add-shutdown-hook [ shutdown-fn ]
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. (fn []
                               (shutdown-fn)))))

(defn- wrap-request-logging [ app development-mode? ]
  (fn [req]
    (if development-mode?
      (log/debug 'REQUEST (:request-method req) (:uri req) (:params req) (:headers req))
      (log/debug 'REQUEST (:request-method req) (:uri req)))

    (let [resp (app req)]
      (if development-mode?
        (log/trace 'RESPONSE (dissoc resp :body))
        (log/trace 'RESPONSE (:status resp)))
      resp)))

(defn get-client-ip [req]
  (if-let [ips (get-in req [:headers "x-forwarded-for"])]
    (-> ips (clojure.string/split #",") first)
    (:remote-addr req)))

(defn- include-requesting-ip [ app ]
  (fn [req]
    (app (assoc req :request-ip (get-client-ip req)))))

(defn- extend-session-duration [ app duration-in-days ]
  (fn [req]
    (assoc (app req) :session-cookie-attrs
           {:max-age (* duration-in-days 24 3600)})))

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
      (extend-session-duration 30)
      (include-requesting-ip)
      (view-common/wrap-remember-query)
      (wrap-dev-support (:development-mode config))
      (handler/site {:session {:store (store/session-store db-conn)}})
      (data/wrap-db-connection db-conn)))

(defn start-site [ routes config db-conn ]
  (let [ { http-port :http-port } config ]
    (log/info "Starting Webserver on port" http-port)
    (let [server (jetty/run-jetty (handler routes config db-conn) { :port http-port :join? false })]
      (add-shutdown-hook
       (fn []
         (log/info "Shutting down webserver")
         (.stop server)))
      (.join server))))

