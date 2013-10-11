(ns toto.view
  (:use hiccup.core)
  (:use clojure.set)
  (:require [toto.data :as data]
            [toto.core :as core]
            [hiccup.form :as form]
            [hiccup.page :as page]))

(def app-name "Toto")

(defn render-page [{ :keys [ page-title include-js sidebar ] }  & contents]
  (let [ t-begin (. System (nanoTime))]
    (html [:html
           [:head
            [:title app-name (if (not (nil? page-title)) (str " - " page-title))]
            [:link { :rel "shortcut icon" :href "/favicon.ico"}]

            (page/include-css "/reset.css")
            (page/include-css "/toto.css")
            (page/include-js "/jquery-1.10.1.js")
            (page/include-js "/jquery-ui.js")
            (page/include-js "/toto.js")
            (map #(page/include-js %) include-js)]

           [:body
            [:div#header 
             [:a { :href "/" } app-name] (if (not (nil? page-title)) (str " - " page-title))
             [:div.right
              (if-let [un (core/authenticated-username)]
                [:span 
                 (str un)
                 " - "
                 [:span#logout
                  [:a { :href "/logout"} "[logout]"]]])]]
            [:div#wrap
             (if sidebar
               (list [:div#sidebar sidebar] [:div#contents contents])
               contents)
             [:div#footer
              "All Rights Reserved, Copyright 2013 East Coast Toolworks "
              (format "(%.1f msec.)" (/ (- (. System (nanoTime)) t-begin) 1000000.0))]]]])))


