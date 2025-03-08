;; Copyright (c) 2015-2025 Michael Schaeffer (dba East Coast Toolworks)
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
  (:use playbook.core
        toto.view.common
        toto.view.components
        toto.view.icons
        toto.view.query
        toto.view.components)
  (:require [taoensso.timbre :as log]
            [hiccup.page :as hiccup-page]
            [hiccup.form :as hiccup-form]
            [hiccup.util :as hiccup-util]
            [playbook.config :as config]
            [toto.site.gdpr :as gdpr]
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


(def without-modal {:modal :remove
                    :edit-item-id :remove
                    :snoozing-item-id :remove})

(defn render-modal [ attrs & contents ]
  (let [{:keys [ title form-post-to data-init-modal ]} attrs
        escape-url (shref without-modal)]
    [:div.dialog-background
     [:dialog {:class "modal" :open "true" :data-escape-url escape-url}
      [:h3 title]
      [:div.cancel
       [:a {:href escape-url} img-window-close]]
      (if form-post-to
        (hiccup-form/form-to
         (if data-init-modal
           {:data-init-modal data-init-modal}
           {})
         [:post form-post-to]
         contents)
        contents)]]))

(defn- render-support-modal [ ]
  (let [user-identity (auth/current-identity)
        friendly-name (auth/current-friendly-name)]
    (render-modal
     {:title "Contact Support"
      :form-post-to "/contact-support"}
     [:div.config-panel
      [:h1 "Contact Information"]
      (hiccup-form/text-field (cond->{:maxlength "128"
                                      :placeholder "Full Name"
                                      :autocomplete "off"
                                      :value friendly-name}
                                friendly-name (assoc :readonly "readonly"))
                              "full-name")
      (hiccup-form/text-field (cond-> {:maxlength "128"
                                       :placeholder "E-Mail Address"
                                       :value user-identity}
                                user-identity (assoc :readonly "readonly"))
                              "email-address")]
     (render-verify-question)
     [:div.config-panel
      [:h1 "Message"]
      (hiccup-form/text-area {:maxlength "4096"
                              :rows "12"
                              :cols "64"
                              :autocomplete "off"
                              :autofocus "on"}
                             "message-text")]
     (hiccup-form/hidden-field "current-uri" (shref))
     [:input {:type "submit" :value "Send Message"}])))

(defn contact-support-button [ ]
  [:a {:href (shref "" {:modal "contact-support" })} "Contact Support"])

(defn- render-standard-header [ attrs ]
  (let [title (:title attrs)
        client-redirect-time (:client-redirect-time attrs)
        client-redirect (:client-redirect attrs)]
    [:head
     [:meta {:name "viewport"
             ;; user-scalable=no fails to work on iOS n where n > 10
             :content "width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0"}]
     (when client-redirect
       [:meta {:http-equiv "refresh"
               :content (str (or client-redirect-time 2) "; url=" client-redirect)}])
     [:title
      (when (config/cval :development-mode) "DEV - ")
      (config/cval :app :title)
      (when title (str " - " title))]
     [:link { :rel "shortcut icon" :href (resource "favicon.ico")}]
     (hiccup-page/include-css
      (resource "toto.css")
      (resource "font-awesome.min.css"))
     [:script {:type "module" :src (resource "toto.js")}]
     (hiccup-page/include-js
      (resource "DragDropTouch.js"))]))

(defn- render-header [ page-title show-menu? ]
  (let [ username (auth/current-identity)]
    [:div.header
     (when show-menu?
       [:span.toggle-menu img-show-list "&nbsp;"])
     [:span.app-name
      [:a { :href "/" } (config/cval :app :title)] " "]
     (when page-title
      (str "- " (hiccup-util/escape-html page-title)))
     (when (config/cval :development-mode)
       [:span.pill.dev "DEV"])
     (session-controls)]))

(defn- render-sidebar-footer []
  [:div.sidebar-footer
   [:div.copyright
    "&#9400; 2015-2025 East Coast Toolworks"]
   (contact-support-button)])

(defn- render-sidebar [ sidebar ]
  [:div.sidebar
   (scroll-column
    "sidebar-scroller"
    [:div.sidebar-control
     [:span.close-menu img-close-list "&nbsp;"]
     (session-controls)]
    sidebar
    (render-sidebar-footer))])

(defn- render-page-modal [ attrs ]
  (when-let [ modal-name (current-modal) ]
    (let [ modal-defns (merge {"contact-support" render-support-modal }
                              (or (:modals attrs) {})) ]
      (if-let [modal (modal-defns modal-name)]
        (modal)
        (log/error "Invalid modal for this page:" modal-name
                   "(known:" (keys modal-defns) ")")))))

(defn- render-gdpr-consent-banner []
  (when (not gdpr/*gdpr-consent*)
    (render-modal
     {:title "Cookie Consent"
      :form-post-to "/user/gdpr-consent"
      :data-turbo "false"}
     [:div
      "This website uses a cookie to remember who you are from visit "
      "to visit. This information is not shared "
      "or used for tracking or advertising purposes."]
     [:div.modal-controls
      [:a {:href "/user/gdpr-consent-decline"} "Decline"]
      (hiccup-form/submit-button {} "Accept")]  )))

(defn- render-page-body [ attrs contents ]
  (let [{ :keys [ title page-data-class sidebar suppress-gdpr-consent ] } attrs ]
    [:body (if page-data-class
             {:data-class page-data-class})
     (render-header title (not (nil? sidebar)))
     (if sidebar
       (render-sidebar sidebar))
     [:div.contents {:class (class-set { "with-sidebar" sidebar })}
      (render-page-modal attrs)
      (when (not suppress-gdpr-consent)
        (render-gdpr-consent-banner))

      contents]]))

(defn render-page [ attrs & contents]
  (hiccup-page/html5
   [:html
    (render-standard-header attrs)
    (render-page-body attrs contents)]))
