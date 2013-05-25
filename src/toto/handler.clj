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
  (POST "/item" {{item-description :item-description} :params}
        (add-item item-description))
  (GET "/users" [] (render-users))
  (GET "/user/:name" [name] (render-user name))

  (route/not-found "<h1>Page not found</h1>"))

(defn is-valid-user? [ user-name pasword ]
  (not (nil? (data/get-user-by-name user-name))))

(def users {"root" {:username "root"
                    :password (credentials/hash-bcrypt "admin_password")
                    :roles #{::admin}}
            "jane" {:username "jane"
                    :password (credentials/hash-bcrypt "user_password")
                    :roles #{::user}}})

(def handler (-> site-routes
                 (friend/authenticate {:credential-fn (partial credentials/bcrypt-credential-fn users)
                                       :workflows [(workflows/interactive-form)]})
                 (handler/site)))