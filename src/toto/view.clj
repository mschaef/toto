(ns toto.view
  (:use toto.util
        clojure.set
        hiccup.core)
  (:require [toto.core :as core]
            [hiccup.form :as form]
            [hiccup.page :as page]))

(def app-name "Toto")

(def img-show-list
     [:img { :src "/list_24x21.png" :class "show-list"
            :width 24 :height 21
            :alt "Show List"}])

(defn logout-button []
  [:span#logout
   [:a { :href "/logout"} "[logout]"]])

(defn standard-includes [ include-js ]
  (list
   (page/include-css "/reset.css"
                     (if (core/is-mobile-request?)
                       "/toto-mobile.css"
                       "/toto-desktop.css")
                     "/font-awesome.min.css")

   (if (core/is-mobile-request?)
     (page/include-js "/zepto.js")
     (page/include-js "/jquery-1.10.1.js" "/jquery-ui.js"))

   (apply page/include-js (cons "/toto.js" include-js))))

(defn standard-header [ page-title include-js ]
  [:head
   (when (core/is-mobile-request?)
     [:meta {:name "viewport"
             :content "width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0"}])
   [:title app-name (unless (nil? page-title) (str " - " page-title))]
   [:link { :rel "shortcut icon" :href "/favicon.ico"}]
   (standard-includes include-js)])

(defn render-footer [ username ]
  [:div#footer
   (when (and (core/is-mobile-request?) (not (nil? username)))
     [:div username " - " (logout-button)])

   "All Rights Reserved, Copyright 2013-15 East Coast Toolworks "])

(defn render-page [{ :keys [ page-title include-js sidebar ] }  & contents]
  (let [username (core/authenticated-username)]
    (html [:html
           (standard-header page-title include-js)

           [:body

            [:div#header 
             (if (core/is-mobile-request?)
               (if username
                 [:a { :href "javascript:toggleSidebar()" :class "click" } img-show-list "&nbsp;"]
                 [:span#vspace "&nbsp;"])
               (list [:a { :href "/" } app-name] " - "))

             page-title

             (unless (or (core/is-mobile-request?) (nil? username))
               [:div.right
                [:span username " - " (logout-button)]])]

            (if sidebar
              (list
               [:div#overlay
                [:div#sidebar sidebar]]
               [:div.wrapper
                [:div#contents
                 contents]])
              [:div#page-contents
               contents])]

           (render-footer username)])))


