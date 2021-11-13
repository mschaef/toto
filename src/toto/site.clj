(ns toto.site
  (:gen-class :main true)
  (:use toto.core.util
        compojure.core)
  (:require [clojure.tools.logging :as log]
            [compojure.route :as route]
            [toto.core.scheduler :as scheduler]
            [toto.core.web :as web]
            [toto.view.user :as user]
            [toto.data.data :as data]
            [toto.todo.todo :as todo]))

(defn all-routes [ config ]
  (log/info "Resources on path: " (str "/" (get-version)))
  (routes
   (route/resources (str "/" (get-version)))
   (user/all-routes config)
   (todo/all-routes config)
   (route/not-found "Resource Not Found")))

(defn schedule-verification-link-cull [ config ]
  (scheduler/schedule-job config "Verification link cull" "*/15 * * * *"
                          #(data/delete-old-verification-links))
  config)

(defn site-start [ config db-conn ]
  (schedule-verification-link-cull config)
  (web/start-site (all-routes config) config db-conn))
