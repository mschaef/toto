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
            [ring.adapter.jetty :as jetty]
            [ring.middleware.file-info :as ring-file-info]
            [ring.middleware.resource :as ring-resource]
            [ring.util.response :as ring-response]
            [cemerick.friend :as friend]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [toto.core :as core]
            [toto.data :as data]
            [toto.todo :as todo]
            [toto.view :as view]            
            [toto.user :as user]))

(defn wrap-request-logging [ app ]
  (fn [req]
    (log/debug 'REQUEST (:request-method req) (:uri req) (:params req))
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

(defn wrap-db-connection [ app ]
  (fn [ req ]
    (data/with-db-connection db
      (app req))))

(defn all-routes [ config ]
  (log/info "Resources on path: " (str "/" (get-version)))
  (routes
   (route/resources (str "/" (get-version)))
   (user/all-routes config)
   (todo/all-routes config)
   (route/not-found "Resource Not Found")))

(defn handler [ config ]
  (-> (all-routes config)
      (wrap-content-type)
      (wrap-browser-caching {"text/javascript" 360000
                             "text/css" 360000})
      (user/wrap-authenticate)
      (extend-session-duration 168)
      (wrap-db-connection)
      (wrap-request-logging)
      (handler/site)))

(defn start-webserver [ config ]
  (let [ { http-port :http-port } config ]
    (log/info "Starting Webserver on port" http-port)
    (let [server (jetty/run-jetty (handler config) { :port http-port :join? false })]
      (add-shutdown-hook
       (fn []
         (log/info "Shutting down webserver")
         (.stop server)))
      (.join server))))

(defn -main [& args]
  (log/info "Starting Toto" (get-version))
  (let [ config (cprop/load-config :resource "config.edn")]
    (log/debug "config" config)    
    (start-webserver config)
    (log/info "end run.")))
