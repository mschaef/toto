(ns toto.core
  (:use hiccup.core)
  (:use clojure.set)
  (:require [clj-http.client :as client]
            [toto.data :as data]))

(defn -main [& args]
  (println "
Please invoke this app as follows:

$ lein2 servlet run"))

(def access-count (atom 0))

(def page-title "Toto")

(defn maybe-update-access-counter [request]
  (if (nil? (.getParameter request "noAccessCount"))
    (reset! access-count (+ 1 (deref access-count)))))

(defn render-current-env-table [request]
  "Return markup that represents a page containing the environment
table."
  (maybe-update-access-counter request)
  (html [:html
         (str "<!-- Access Count:"  (deref access-count)  " -->")
         [:title page-title]
         [:body
          [:p "We're not in Kansas anymore."]
          [:list
           (map (fn [table-name]
                  [:li table-name])
                (data/all-table-names))]]]))

