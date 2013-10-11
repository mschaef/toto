(ns toto.handler
  (:use compojure.core)
  (:require [clojure.tools.logging :as log]
            [toto.data :as data]
            [toto.core :as core]
            [toto.user :as user]
            [toto.todo :as todo]
            [cemerick.friend :as friend]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [cemerick.friend.workflows :as workflows]
            [cemerick.friend.credentials :as credentials]))

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

(def handler (-> site-routes
                 (friend/authenticate {:credential-fn db-credential-fn
                                       :workflows [(workflows/interactive-form)]})
                 (extend-session-duration 168)
                 (handler/site)
                 (wrap-request-logging)))