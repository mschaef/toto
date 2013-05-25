(ns toto.handler
  (:use toto.core)
  (:use compojure.core)
  (:require [toto.data :as data]
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
  (GET "/users" [] (render-users))
  (GET "/user/:name" [name] (render-user name))

  (route/not-found "<h1>Page not found</h1>"))


(defn db-credential-fn [ creds ]
  (let [user-record (data/get-user-by-name (creds :username))]
    (if (or (nil? user-record)
            (not (credentials/bcrypt-verify (creds :password) (user-record :password))))
      nil
      { :identity (creds :username) })))

(def handler (-> site-routes
                 (friend/authenticate {:credential-fn db-credential-fn
                                       :workflows [(workflows/interactive-form)]})
                 (handler/site)))