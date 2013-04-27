(ns toto.handler
  (:use toto.core)
  (:use compojure.core)
  (:require [compojure.route :as route]
            [compojure.handler :as handler]))


(defroutes site-routes
  (GET "/" [] (render-todo-list))
  (POST "/item" {{item-description :item-description} :params}
        (add-item item-description))
  (GET "/users" [] (render-users))
  (GET "/user/:name" [name] (render-user name))

  (route/not-found "<h1>Page not found</h1>"))

(def handler (handler/site site-routes))