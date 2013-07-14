(ns toto.view
  (:use hiccup.core)
  (:use clojure.set)
  (:require [toto.data :as data]
            [toto.core :as core]
            [hiccup.form :as form]
            [hiccup.page :as page]))

(def page-title "Toto")

(defn render-page [& contents]
  (html [:html
         [:head
          [:title page-title]       
          (page/include-css "/reset.css")
          (page/include-css "/toto.css")
          (page/include-js "/jquery-1.10.1.js")]
         [:body
          [:div#header 
           "Things to do"
           [:div.right
            (if (not (nil? core/*username*))
              [:span 
               (str core/*username*)
               " - "
               [:span#logout
                [:a { :href "/logout"} "[logout]"]]])]]
          [:div#wrap
           contents
           [:div#footer
            "All Rights Reserved, Copyright 2013 East Coast Toolworks"]]]]))

