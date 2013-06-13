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

(defroutes auth-routes
  (friend/logout (ANY "/logout" []  (ring.util.response/redirect "/")))

  (GET "/login" [] (render-login-page))
  (route/resources "/"))

(defroutes secure-site-routes
  (GET "/" [] (render-todo-list))

  (POST "/item" {{item-description :item-description} :params}
        (add-item item-description))

  (GET "/item/:id" [id]
       (render-item id))

  (POST "/item/:id"  {{id :id description :description} :params}
        (update-item id description))

  (POST "/item/:id/complete" [id]
       (complete-item id))

  (GET "/user" []
       (render-new-user-form))

  (POST "/user" {{email-addr :email_addr password :password} :params}
        (add-user email-addr password))

  (GET "/users" [] (render-users))

  (GET "/user/:id" [id] (render-user id)))

(def site-routes
     (routes auth-routes
             (fn [req] (friend/authenticated (secure-site-routes req)))
             ))

(defn wrap-username [app]
  (fn [req]
    (binding [core/*username* (if-let [cauth (friend/current-authentication req)]
                                (cauth :identity)
                                nil)]
      (app req))))

(defn db-credential-fn [ creds ]
  (let [user-record (data/get-user-by-email (creds :username))]
    (if (or (nil? user-record)
            (not (credentials/bcrypt-verify (creds :password) (user-record :password))))
      nil
      { :identity (creds :username) })))

(def handler (-> site-routes
                 (wrap-username)
                 (friend/authenticate {:credential-fn db-credential-fn
                                       :workflows [(workflows/interactive-form)]})
                 (handler/site)))