(ns toto.handler
  (:use toto.core)
  (:use compojure.core)
  (:require [toto.data :as data]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.middleware.basic-authentication :as basic-auth]
            ))

(defroutes site-routes
  (GET "/" [] (render-todo-list))
  (GET "/login" [] (render-login-page))
  (POST "/item" {{item-description :item-description} :params}
        (add-item item-description))
  (GET "/users" [] (render-users))
  (GET "/user/:name" [name] (render-user name))

  (route/not-found "<h1>Page not found</h1>"))

(defn is-valid-user? [ user-name pasword ]
  (not (nil? (data/get-user-by-name user-name))))

(def handler (basic-auth/wrap-basic-authentication (handler/site site-routes) is-valid-user?))