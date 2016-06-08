(ns toto.view
  (:use toto.util
        clojure.set
        hiccup.core)
  (:require [toto.core :as core]
            [clojure.data.json :as json]
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

(defn standard-includes [ init-map ]
  (list
   (page/include-css (if (core/is-mobile-request?)
                       (resource "toto-mobile.css")
                       (resource "toto-desktop.css"))
                     (resource "font-awesome.min.css"))

   (if (core/is-mobile-request?)
     (page/include-js (resource "zepto.js"))
     (page/include-js (resource "jquery-1.10.1.js")
                      (resource "jquery-ui.js")))
   (page/include-js (resource "toto.js"))

   [:script
    "totoInitialize(" (json/write-str init-map) ");"]))

(defn standard-header [ page-title init-map ]
  [:head
   (when (core/is-mobile-request?)
     [:meta {:name "viewport"
             :content "width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0"}])
   [:title app-name (unless (nil? page-title) (str " - " page-title))]
   [:link { :rel "shortcut icon" :href (resource "/favicon.ico")}]
   (standard-includes init-map)])

(defn render-footer [ username ]
  [:div#footer
   (when (and (core/is-mobile-request?) (not (nil? username)))
     [:div username " - " (logout-button)])

   "All Rights Reserved, Copyright 2013-16 East Coast Toolworks "])

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
     [:div#sidebar
      sidebar
      (render-footer username)])
   (list
    (render-header page-title)
    [:div#contents
     [:div#contents-container
      contents]])])

(defn render-desktop-page-body [ page-title username sidebar contents ]
  [:body
   (if sidebar
     [:div#sidebar sidebar])
   (render-header page-title)
   [(if sidebar :div#contents :div#page-contents)
    [:div#contents-container
     contents
     (render-footer username)]]])

(defn render-page [{ :keys [ page-title init-map sidebar ] }  & contents]
  (let [username (core/authenticated-username)]
    (html [:html
           (standard-header page-title (merge {} init-map))
           ((if (core/is-mobile-request?)
              render-mobile-page-body
              render-desktop-page-body)
            page-title username sidebar contents)])))


