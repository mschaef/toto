(ns toto.handler
  (:use toto.core)
  (:use compojure.core)
  (:require [compojure.route :as route]
            [compojure.handler :as handler]))


(defroutes site-routes
  (GET "/" [] (render-current-env-table))
  (GET "/users" [] (render-users))

  (route/not-found "<h1>Page not found</h1>"))

(def handler (handler/site site-routes))