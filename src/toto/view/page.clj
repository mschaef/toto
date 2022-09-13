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
        toto.view.icons
        toto.view.query)
  (:require [clojure.tools.logging :as log]
            [hiccup.page :as page]
            [hiccup.form :as form]
            [toto.view.auth :as auth]))

(defn session-controls []
  (if-let [ username (auth/current-identity) ]
    [:div.session-controls
     [:a {:href "/user/info"} username]
     " - "
     [:a.warning { :href "/logout"} "Log Out"]]
    [:div.session-controls
     [:a {:href "/login"} "Sign In"]
     [:a.emphasize {:href "/user"} "Sign Up"]]))

(defn- resource [ path ]
  (str "/" (get-version) "/" path))

(defn- render-standard-header [ title ]
  [:head
   [:meta {:name "viewport"
           ;; user-scalable=no fails to work on iOS n where n > 10
           :content "width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0"}]
   [:title (when *dev-mode* "DEV - ") (:name (:app *config*)) (when title (str " - " title))]
   [:link { :rel "shortcut icon" :href (resource "favicon.ico")}]
   (page/include-css (resource "toto.css")
                     (resource "font-awesome.min.css"))
   [:script {:type "module" :src (resource "toto.js")}]
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
     (session-controls)]))

(defn- render-sidebar [ sidebar ]
  [:div.sidebar
   [:div.sidebar-control
    [:span.close-menu img-close-list "&nbsp;"]
    (session-controls)]
   [:div.sidebar-content { :id "sidebar-scroller" :data-preserve-scroll "true" }
    sidebar
    [:div.copyright
     "&#9400; 2015-2022 East Coast Toolworks"]]])

(def without-modal {:modal :remove
                    :edit-item-id :remove
                    :snoozing-item-id :remove})

(defn render-modal [ attrs & contents ]
  (let [ escape-url (shref without-modal)]
    [:div.dialog-background
     [:dialog {:open "true" :data-escape-url escape-url}
      [:h3 (:title attrs)]
      [:div.cancel
       [:a {:href escape-url} img-window-close]]
      (if-let [ form-post-to (:form-post-to attrs)]
        (form/form-to [:post form-post-to] contents)
        contents)]]))

(defn- render-page-modal [ attrs ]
  (when-let [ modal-name (current-modal) ]
    (let [ modal-defns (or (:modals attrs) {}) ]
      (if-let [modal (modal-defns modal-name)]
        (modal)
        (log/error "Invalid modal for this page:" modal-name
                   "(known:" (keys modal-defns) ")")))))

(defn- render-page-body [ attrs contents ]
  (let [{ :keys [ title page-data-class sidebar ] } attrs ]
    [:body (if page-data-class
             {:data-class page-data-class})
     (render-header title (not (nil? sidebar)))
     (if sidebar
       (render-sidebar sidebar))
     [:div.contents {:class (class-set { "with-sidebar" sidebar })}
      (render-page-modal attrs)
      contents]]))

(defn render-page [ attrs & contents]
  (html [:html
         (render-standard-header (:title attrs))
         (render-page-body attrs contents)]))
