(ns toto.handler
  (:use compojure.core)
  (:require [toto.data :as data]
            [toto.core :as core]
            [toto.user :as user]
            [toto.todo :as todo]
            [cemerick.friend :as friend]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [cemerick.friend.workflows :as workflows]
            [cemerick.friend.credentials :as credentials]))

(def site-routes
     (routes
      user/public-routes
      (route/resources "/")
      (fn [req] (friend/authorize #{::user} (todo/all-routes req)))
      (route/not-found "Resource Not Found")))

(defn db-credential-fn [ creds ]
  (let [user-record (data/get-user-by-email (creds :username))]
    (if (or (nil? user-record)
            (not (credentials/bcrypt-verify (creds :password) (user-record :password))))
      nil
      { :identity (creds :username) :roles #{ ::user }})))

(def handler (-> site-routes
                 ;(core/wrap-logging)
                 (core/wrap-username)
                 (friend/authenticate {:credential-fn db-credential-fn
                                       :workflows [(workflows/interactive-form)]})
                 (handler/site)))