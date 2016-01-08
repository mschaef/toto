(ns toto.main
  (:gen-class :main true)
  (:use toto.util
        compojure.core)
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]            
            [ring.adapter.jetty :as jetty]
            [ring.middleware.file-info :as ring-file-info]
            [ring.middleware.resource :as ring-resource]
            [ring.util.response :as ring-response]
            [cemerick.friend :as friend]
            [cemerick.friend.credentials :as credentials]
            [cemerick.friend.workflows :as workflows]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [toto.core :as core]
            [toto.data :as data]
            [toto.todo :as todo]
            [toto.user :as user]))

(defmacro get-version []
  ;; Capture compile-time property definition from Lein
  (System/getProperty "toto.version"))

(def site-routes (routes user/public-routes
                         (route/resources "/")
                         (friend/wrap-authorize todo/all-routes #{::user})
                         (route/not-found "Resource Not Found")))

(defn db-credential-fn [ creds ]
  (let [user-record (data/get-user-by-email (creds :username))]
    (if (or (nil? user-record)
            (not (credentials/bcrypt-verify (creds :password) (user-record :password))))
      nil
      { :identity (creds :username) :roles #{ ::user }})))

(defn wrap-request-logging [ app ]
  (fn [req]
    (log/debug 'REQUEST (:request-method req) (:uri req))
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

(defn wrap-cache-control [handler cache-control]
  (fn [request]
    (let [response (handler request)]
      (if (instance? java.io.File (:body response))
        (-> response (ring-response/header "CacheControl" cache-control))
        response))))

(def handler (-> site-routes
                 (ring-resource/wrap-resource "public")
                 (ring-file-info/wrap-file-info)
                 (friend/authenticate {:credential-fn db-credential-fn
                                       :workflows [(workflows/interactive-form)]})
                 (extend-session-duration 168)
                 (wrap-db-connection)
                 (wrap-request-logging)
                 (core/wrap-mobile-detect)
                 (handler/site)))


(defn start-webserver [ http-port ]
  (log/info "Starting Webserver on port" http-port)
  (let [server (jetty/run-jetty handler { :port http-port :join? false })]
    (add-shutdown-hook
     (fn []
       (log/info "Shutting down webserver")
       (.stop server)))
    (.join server)))

(defn -main [& args]
  (log/info "Starting Toto" (get-version))
  (start-webserver (config-property "http.port" 8080))
  (log/info "end run."))
