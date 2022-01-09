;; Copyright (c) 2015-2022 Michael Schaeffer (dba East Coast Toolworks)
;;
;; Licensed as below.
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;       http://www.apache.org/licenses/LICENSE-2.0
;;
;; The license is also includes at the root of the project in the file
;; LICENSE.
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.
;;
;; You must not remove this notice, or any other, from this software.

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
   [:title (:name (:app *config*)) (when page-title (str " - " page-title))]
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
      [:a { :href "/" } (:name (:app *config*))] " - "]
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
   [:div.sidebar-content { :id "sidebar-scroller" :data-preserve-scroll "true" }
    sidebar
    [:div.copyright
     "&#9400; 2015-2022 East Coast Toolworks"]]])

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
