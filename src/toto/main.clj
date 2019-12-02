(ns toto.main
  (:gen-class :main true)
  (:use toto.util
        compojure.core
        [ring.middleware resource
         not-modified
         content-type
         browser-caching])
  (:require [clojure.tools.logging :as log]
            [cprop.core :as cprop]
            [cprop.source :as cprop-source]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.reload :as ring-reload]
            [ring.middleware.file-info :as ring-file-info]
            [ring.middleware.resource :as ring-resource]
            [ring.util.response :as ring-response]
            [cemerick.friend :as friend]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [toto.data :as data]
            [toto.todo :as todo]
            [toto.view :as view]
            [toto.view-utils :as view-utils]
            [toto.user :as user]))

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


(defn all-routes [ config ]
  (log/info "Resources on path: " (str "/" (get-version)))
  (routes
   (route/resources (str "/" (get-version)))
   (user/all-routes config)
   (todo/all-routes config)
   (route/not-found "Resource Not Found")))

(defn handler [ config ]
  (cond-> (ring-reload/wrap-reload
           (-> (all-routes config)
               (wrap-content-type)
               (wrap-browser-caching {"text/javascript" 360000
                                      "text/css" 360000})
               (user/wrap-authenticate)
               (extend-session-duration 168)
               (data/wrap-db-connection)
               (wrap-request-logging (:development-mode config))
               (view-utils/wrap-remember-params)
               (handler/site)))
    (:development-mode config) (ring-reload/wrap-reload)))

(defn start-webserver [ config ]
  (let [ { http-port :http-port } config ]
    (when (:development-mode config)
      (log/warn "=== DEVELOPMENT MODE ==="))
    (log/info "Starting Webserver on port" http-port)
    (let [server (jetty/run-jetty (handler config) { :port http-port :join? false })]
      (add-shutdown-hook
       (fn []
         (log/info "Shutting down webserver")
         (.stop server)))
      (.join server))))

(defn maybe-config-file [ prop-name ]
  (if-let [prop (System/getProperty prop-name)]
    (if (.exists (java.io.File. prop))
      (do
        (log/info (str "Config file found: " prop "(specified by property: " prop-name ")"))
        (cprop-source/from-file prop))
      (do
        (log/warn (str "CONFIG FILE NOT FOUND: " prop "(specified by property: " prop-name ")"))
        {}))
    {}))

(defn -main [& args]
  (log/info "Starting Toto" (get-version))
  (let [config (cprop/load-config :merge [(cprop-source/from-resource "config.edn")
                                          (maybe-config-file "conf")
                                          (maybe-config-file "creds")])]
    (log/debug "config" config)    
    (start-webserver config)
    (log/info "end run.")))
