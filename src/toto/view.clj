(ns toto.view
  (:use hiccup.core)
  (:use clojure.set)
  (:require [toto.data :as data]
            [toto.core :as core]
            [hiccup.form :as form]
            [hiccup.page :as page]
            [ring.util.response :as ring]
            [cemerick.friend.credentials :as credentials]))

(def page-title "Toto")

(defn render-page [& contents]
  (html [:html
         [:head
          [:title page-title]
          (page/include-css "/toto.css")
          (page/include-js "/jquery-1.10.1.js")
          ]
         [:body
          [:div#header
           "Things to do"
             (if (not (nil? core/*username*))
               (str " - " core/*username*))]
          [:div#wrap
           contents
           [:div#footer
            [:hr]
            [:center
             "All Rights Reserved, Copyright 2013 East Coast Toolworks"
             (if (not (nil? core/*username*))
               [:span#logout
                " - "
                [:a { :href "/logout"} "[logout]"]])]]]]]))

(defn render-login-page [ & { :keys [ email-addr login-failure?]}]

  (render-page (form/form-to
                [:post "/login"]
                [:table
                 [:tr [:td { :colspan 2 } [:center "Login to Toto"]]]
                 [:tr [:td "E-Mail Address:"] [:td (form/text-field {} "username" (if email-addr email-addr))]]
                 [:tr [:td "Password:"] [:td (form/password-field {} "password")]]
                 (if login-failure?
                   [:tr [:td { :colspan 4 } [:div#error "Invalid username or password."]]])
                 [:tr 
                  [:td { :colspan 4 }
                   [:center
                    [:a { :href "/user"} "Create New User"]
                    " - "
                    (form/submit-button {} "Login")]]]])))


