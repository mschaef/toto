(ns toto.view
  (:use toto.util
        clojure.set
        hiccup.core)
  (:require [toto.core :as core]
            [hiccup.form :as form]
            [hiccup.page :as page]))

(def app-name "Toto")

(def img-show-list
  [:i {:class "fa fa-bars fa-lg icon-bars"}])

(defn logout-button []
  [:span#logout
   [:a { :href "/logout"} "[logout]"]])

(defn resource [ path ]
  (str "/" (get-version) "/" path))

(defn standard-includes [ include-js ]
  (list
   (page/include-css (resource "reset.css")
                     (if (core/is-mobile-request?)
                       (resource "toto-mobile.css")
                       (resource "toto-desktop.css"))
                     (resource "font-awesome.min.css"))

   (if (core/is-mobile-request?)
     (page/include-js (resource "zepto.js"))
     (page/include-js (resource "jquery-1.10.1.js")
                      (resource "jquery-ui.js")))

   (apply page/include-js (map resource (cons "toto.js" include-js)))))

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

(defn render-header [ page-title]
  (let [ username (core/authenticated-username)]
    [:div#header 
     (if (core/is-mobile-request?)
       (if username
         [:span#toggle-menu  img-show-list "&nbsp;"]
         [:span#vspace "&nbsp;"])
       (list [:a { :href "/" } app-name] " - "))

     page-title

     (unless (or (core/is-mobile-request?) (nil? username))
       [:div.right
        [:span username " - " (logout-button)]])]))

(defn render-mobile-page-body [ page-title username sidebar contents ]
  [:body
   (if sidebar
     [:div#sidebar sidebar])
   [:div#contents
    (render-header page-title)
    contents
    (render-footer username)]])

(defn render-desktop-page-body [ page-title username sidebar contents ]
  [:body
   (if sidebar
     [:div#sidebar sidebar])
   (render-header page-title)
   [(if sidebar :div#contents :div#page-contents)
    contents]
   (render-footer username)])

(defn render-page [{ :keys [ page-title include-js sidebar ] }  & contents]
  (let [username (core/authenticated-username)]
    (html [:html
           (standard-header page-title include-js)
           ((if (core/is-mobile-request?)
              render-mobile-page-body
              render-desktop-page-body)
            page-title username sidebar contents)])))


