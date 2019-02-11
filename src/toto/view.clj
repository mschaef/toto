(ns toto.view
  (:use toto.util
        clojure.set
        hiccup.core)
  (:require [toto.core :as core]
            [clojure.data.json :as json]
            [cemerick.friend :as friend]            
            [hiccup.form :as form]
            [hiccup.page :as page]))

(def app-name "Toto")

(def img-show-list
  [:i {:class "fa fa-bars fa-lg icon-bars"}])

(def img-close-list
  [:i {:class "fa fa-times-circle fa-lg"}])

(defn- logout-button []
  [:span#logout
   [:a { :href "/logout"} "[logout]"]])

(defn- resource [ path ]
  (str "/" (get-version) "/" path))

(defn- standard-includes [ init-map ]
  (list
   (page/include-css (resource "toto.css")
                     (resource "font-awesome.min.css"))
   (page/include-js (resource "toto.js"))
   [:script
    "totoInitialize(" (json/write-str init-map) ");"]))

(defn- render-standard-header [ page-title init-map ]
  [:head
   [:meta {:name "viewport"
           :content "width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0"}]
   [:title app-name (when page-title (str " - " page-title))]
   [:link { :rel "shortcut icon" :href (resource "favicon.ico")}]
   (standard-includes init-map)])

(defn- render-footer [ username ]
  [:div#footer
   "All Rights Reserved, Copyright 2013-19 East Coast Toolworks "
   (when username
     [:div.logout username " - " (logout-button)])])

(defn- render-header [ page-title username ]
  [:div#header
   [:span#toggle-menu img-show-list "&nbsp;"]
   [:span.app-name
    [:a { :href "/" } app-name] " - "]
   page-title
   (when username
     [:div.right
      [:span.logout username [:span.logout-control " - " (logout-button)]]])] )

(defn- render-sidebar [ username sidebar ]
  [:div#sidebar
   [:div.sidebar-control
    [:span#close-menu  img-close-list "&nbsp;"]
    [:span.username username]]
   sidebar])

(defn- render-page-body [ page-title username sidebar contents ]
  [:body
   (if sidebar
     (render-sidebar username sidebar))
   (render-header page-title username)
   [:div#contents {:class (class-set { "with-sidebar" sidebar })}
    [:div#contents-container
     contents]]
   (render-footer username)])

(defn render-page [{ :keys [ page-title init-map sidebar ] }  & contents]
  (let [username (get (friend/current-authentication) :identity)]
    (html [:html
           (render-standard-header page-title (merge {} init-map))
           (render-page-body page-title username sidebar contents)])))


