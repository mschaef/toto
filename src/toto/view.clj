(ns toto.view
  (:use toto.util
        clojure.set
        hiccup.core
        toto.view-utils)
  (:require [cemerick.friend :as friend]
            [hiccup.form :as form]
            [hiccup.page :as page]))

(def app-name "Toto")

(def img-show-list
  [:i {:class "fa fa-bars fa-lg icon-bars"}])

(def img-close-list
  [:i {:class "fa fa-times-circle fa-lg"}])

(defn- logout-button []
  [:span.logout
   [:a { :href "/logout"} "[logout]"]])

(defn- login-button []
  [:span.login
   [:a { :href "/"} "[login]"]])

(defn- resource [ path ]
  (str "/" (get-version) "/" path))

(defn- render-standard-header [ page-title ]
  [:head
   [:meta {:name "viewport"
           ;; user-scalable=no fails to work on iOS n where n > 10
           :content "width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0"}]
   [:title app-name (when page-title (str " - " page-title))]
   [:link { :rel "shortcut icon" :href (resource "favicon.ico")}]
   (page/include-css (resource "toto.css")
                     (resource "font-awesome.min.css"))
   (page/include-js "https://cdnjs.cloudflare.com/ajax/libs/turbolinks/5.2.0/turbolinks.js")
   (page/include-js (resource "toto.js"))
   (page/include-js (resource "DragDropTouch.js"))])

(defn- render-footer [ username ]
  [:div.footer
   "&#9400; 2020 East Coast Toolworks "
   (when username
     [:div.logout username " - " (logout-button)])])

(defn- render-header [ page-title username show-menu? ]
  [:div.header
   (when show-menu?
     [:span.toggle-menu img-show-list "&nbsp;"])
   [:span.app-name
    [:a { :href "/" } app-name] " - "]
   page-title
   (when *dev-mode*
     [:span.pill.dev "DEV"])
   (if username
     [:div.right
      [:span.logout
       [:a {:href "/user/info"} username]
       [:span.logout-control " - " (logout-button)]]]
     [:div.right
      (login-button)])])

(defn- render-sidebar [ username sidebar ]
  [:div.sidebar
   [:div.sidebar-control
    [:span.close-menu  img-close-list "&nbsp;"]
    [:span.logout
     [:a {:href "/user/password-change"} username]
     [:span.logout-control " - " (logout-button)]]]
   [:div#sidebar.sidebar-content { :data-preserve-scroll "true" }
    sidebar
    [:div.copyright
     "&#9400; 2015-2020 East Coast Toolworks "]]])

(defn- render-page-body [ page-data-class page-title username sidebar contents ]
  [:body (if page-data-class
           {:data-class page-data-class})
   (render-header page-title username (not (nil? sidebar)))
   (if sidebar
     (render-sidebar username sidebar))
   [:div.contents {:class (class-set { "with-sidebar" sidebar })}
    contents]])

(defn render-page [{ :keys [ page-title page-data-class sidebar ] }  & contents]
  (let [username (current-identity)]
    (html [:html
           (render-standard-header page-title)
           (render-page-body page-data-class page-title username sidebar contents)])))


