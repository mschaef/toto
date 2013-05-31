(ns toto.handler
  (:use toto.view)
  (:use compojure.core)
  (:require [toto.data :as data]
            [toto.core :as core]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [cemerick.friend :as friend]
            [cemerick.friend.workflows :as workflows]
            [cemerick.friend.credentials :as credentials]))

(defroutes site-routes
  (GET "/" [] (friend/authenticated (render-todo-list)))

  (GET "/login" [] (render-login-page))

  (friend/logout (ANY "/logout" request (ring.util.response/redirect "/")))

  (POST "/item" {{item-description :item-description} :params}
        (add-item item-description))

  (GET "/item/:id" [id]
       (render-item id))

  (POST "/user" {{username :username
                  email-addr :email_addr
                  password :password}
                 :params}
        (add-user username email-addr password))

  (GET "/users" [] (render-users))
  (GET "/user/:name" [name] (render-user name))

  (route/not-found "<h1>Page not found</h1>"))

(defn wrap-username [app]
  (fn [req]
    (binding [core/*username* (if-let [cauth (friend/current-authentication req)]
                                (cauth :identity)
                                nil)]
      (app req))))

(defn db-credential-fn [ creds ]
  (let [user-record (data/get-user-by-name (creds :username))]
    (if (or (nil? user-record)
            (not (credentials/bcrypt-verify (creds :password) (user-record :password))))
      nil
      { :identity (creds :username) })))

(def handler (-> site-routes
                 (wrap-username)
                 (friend/authenticate {:credential-fn db-credential-fn
                                       :workflows [(workflows/interactive-form)]})
                 (handler/site)))