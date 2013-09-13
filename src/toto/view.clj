(ns toto.view
  (:use hiccup.core)
  (:use clojure.set)
  (:require [toto.data :as data]
            [toto.core :as core]
            [hiccup.form :as form]
            [hiccup.page :as page]))

(def page-title "Toto")

(defn render-page [{ :keys [ page-title ] } & contents]
  (html [:html
         [:head
          [:title (if (nil? page-title)
                    "Toto"
                    (str "Toto - " page-title))]
          (page/include-css "/reset.css")
          (page/include-css "/toto.css")
          (page/include-js "/jquery-1.10.1.js")
          (page/include-js "/toto.js")
          [:link { :rel "shortcut icon" :href "/favicon.ico"}]]
         [:body
          [:div#header 
           [:a { :href "/" } "Toto"]
           (if (not (nil? page-title))
             (str " - " page-title))
           [:div.right
            (if-let [un (core/authenticated-username)]
              [:span 
               (str un)
               " - "
               [:span#logout
                [:a { :href "/logout"} "[logout]"]]])]]
          [:div#wrap
           contents
           [:div#footer
            "All Rights Reserved, Copyright 2013 East Coast Toolworks"]]]]))


