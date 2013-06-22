(ns toto.view
  (:use hiccup.core)
  (:use clojure.set)
  (:require [toto.data :as data]
            [toto.core :as core]
            [hiccup.form :as form]
            [ring.util.response :as ring]
            [cemerick.friend.credentials :as credentials]))

(def page-title "Toto")

(defn render-page [& contents]
  (html [:html
         [:title page-title]
         [:body contents]
         [:hr]
         (if (not (nil? core/*username*))
           [:span
            [:a { :href "/logout"} "logout"]
            (str " - " core/*username*)]
           [:a { :href "/user"} "New User"])]))

(defn render-login-page []
  (render-page (form/form-to
                [:post "/login"]
                [:table
                 [:tr [:td { :colspan 2 } [:center "Login to Toto"]]]
                 [:tr [:td "E-Mail Address:"] [:td (form/text-field {} "username")]]
                 [:tr [:td "Password:"] [:td (form/password-field {} "password")]]
                 [:tr [:td ] [:td (form/submit-button {} "Login")]]])))


