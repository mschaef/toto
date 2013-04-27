(ns toto.handler
  (:use toto.core)
  (:use compojure.core)
  (:require [compojure.route :as route]))


(defroutes handler
  (GET "/" [] (render-current-env-table))
  (route/not-found "<h1>Page not found</h1>"))
