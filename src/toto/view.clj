(ns toto.view
  (:use hiccup.core)
  (:use clojure.set)
  (:require [toto.core :as core]
            [hiccup.form :as form]
            [hiccup.page :as page]))

(def app-name "Toto")

(def img-show-list
     [:img { :src "/list_24x21.png" :class "show-list"
            :width 24 :height 21
            :alt "Show List"}])

(defn render-page [{ :keys [ page-title include-js sidebar ] }  & contents]
  (let [ t-begin (. System (nanoTime))
        username (core/authenticated-username)]
    (html [:html
           [:head
            (when (core/is-mobile-request?)
              [:meta {:name "viewport" :content "width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0"}])
            [:title app-name (if (not (nil? page-title)) (str " - " page-title))]
            [:link { :rel "shortcut icon" :href "/favicon.ico"}]

            (page/include-css "/reset.css")
            (page/include-css (if (core/is-mobile-request?)
                                "/toto-mobile.css"
                                "/toto-desktop.css"))
            (page/include-js "/jquery-1.10.1.js")
            (page/include-js "/jquery-ui.js")
            (page/include-js "/toto.js")

            (map #(page/include-js %) include-js)]

           [:body
            [:div#header 
             (if (core/is-mobile-request?)
               [:a { :href "#" :class "click" } img-show-list "&nbsp;"]
               (list
                [:a { :href "/" } app-name]
                " - "))

             page-title

             [:div.right
              (if (and (not (core/is-mobile-request?))
                       (not (nil? username)))
                [:span
                 (str username)
                 " - "
                 [:span#logout
                  [:a { :href "/logout"} "[logout]"]]])]]

            [:div#wrap
             (if sidebar
               (list [:div#sidebar sidebar] [:div#contents contents])
               contents)
             [:div#footer
              "All Rights Reserved, Copyright 2013 East Coast Toolworks "
              (format "(%.1f msec.)" (/ (- (. System (nanoTime)) t-begin) 1000000.0))
              (if (and (core/is-mobile-request?)
                       (not (nil? username)))
                (list
                 " - "
                 username
                 " - "
                 [:span#logout
                  [:a { :href "/logout"} "[logout]"]]))]]]])))


