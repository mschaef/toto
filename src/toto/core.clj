(ns toto.core
  (:use hiccup.core)
  (:use clojure.set)
  (:require [clj-http.client :as client]
            [toto.data :as data]))

(defn -main [& args]
  (println "
Please invoke this app as follows:

$ lein2 servlet run"))


(def page-title "Toto")

(defn render-page [& contents]
  (html [:html
         [:title page-title]
         [:body contents]]))

(defn render-current-env-table []
  "Return markup that represents a page containing the environment
table."
  (render-page [:p "We're not in Kansas anymore."]
               [:table
                (map (fn [table-name]
                       [:tr [:td table-name]])
                     (data/all-table-names))]))

(defn render-users []
  (render-page [:h1 "List of Users"]
               [:ul
                (map (fn [user-name]
                       [:li user-name])
                     (data/all-user-names))]))