(ns toto.view.page
  (:use hiccup.core
        toto.core.util
        toto.view.common
        toto.view.icons)
  (:require [hiccup.page :as page]
            [toto.view.auth :as auth]))

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
   [:title (:app-name *config*) (when page-title (str " - " page-title))]
   [:link { :rel "shortcut icon" :href (resource "favicon.ico")}]
   (page/include-css (resource "toto.css")
                     (resource "font-awesome.min.css"))
   (page/include-js (resource "turbolinks.js"))
   (page/include-js (resource "toto.js"))
   (page/include-js (resource "DragDropTouch.js"))])

(defn- render-header [ page-title show-menu? ]
  (let [ username (auth/current-identity)]
    [:div.header
     (when show-menu?
       [:span.toggle-menu img-show-list "&nbsp;"])
     [:span.app-name
      [:a { :href "/" } (:app-name *config*)] " - "]
     page-title
     (when *dev-mode*
       [:span.pill.dev "DEV"])
     (if username
       [:div.right
        [:span.logout
         [:a {:href "/user/info"} username]
         [:span.logout-control " - " (logout-button)]]]
       [:div.right
        (login-button)])]))

(defn- render-sidebar [ sidebar ]
  [:div.sidebar
   [:div.sidebar-control
    [:span.close-menu img-close-list "&nbsp;"]
    [:span.logout
     [:a {:href "/user/password-change"} (auth/current-identity)]
     [:span.logout-control " - " (logout-button)]]]
   [:div.sidebar-content { :data-preserve-scroll "true" }
    sidebar
    [:div.copyright
     "&#9400; 2015-2021 East Coast Toolworks"]]])

(defn render-modal [ escape-url & contents ]
  [:div.modal-background
   [:div.modal {:data-escape-url escape-url}
    [:div.cancel
     [:a {:href escape-url} img-window-close]]
    contents]])

(defn- render-page-body [ page-data-class page-title sidebar contents ]
  [:body (if page-data-class
           {:data-class page-data-class})
   (render-header page-title (not (nil? sidebar)))
   (if sidebar
     (render-sidebar sidebar))
   [:div.contents {:class (class-set { "with-sidebar" sidebar })}
    contents]])

(defn render-page [{ :keys [ page-title page-data-class sidebar ] }  & contents]
  (html [:html
         (render-standard-header page-title)
         (render-page-body page-data-class page-title sidebar contents)]))
