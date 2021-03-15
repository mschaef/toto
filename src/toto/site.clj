(ns toto.site
  (:gen-class :main true)
  (:use toto.core.util
        compojure.core)
  (:require [clojure.tools.logging :as log]
            [compojure.route :as route]
            [toto.core.web :as web]
            [toto.view.todo :as todo]
            [toto.view.user :as user]))

(defn all-routes [ config ]
  (log/info "Resources on path: " (str "/" (get-version)))
  (routes
   (route/resources (str "/" (get-version)))
   (user/all-routes config)
   (todo/all-routes config)
   (route/not-found "Resource Not Found")))

(defn site-start [ config db-conn ]
  (web/start-site (all-routes config) config db-conn))
