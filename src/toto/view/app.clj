(ns toto.view.app
  (:use toto.core.util
        compojure.core
        toto.view.page)
  (:require [cemerick.friend :as friend]))

(defn render-app-toplevel []
  (render-page { :page-title "App Toplevel"}
               [:div.page-message
                "App Toplevel"]))

(defn private-routes [ config ]
  (routes
   (GET "/" []
     (render-app-toplevel))))

(defn all-routes [ config ]
  (routes
   (wrap-routes (private-routes config)
                friend/wrap-authorize
                #{:toto.role/verified})))
