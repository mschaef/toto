(ns toto.view
  (:use hiccup.core)
  (:use clojure.set)
  (:require [toto.data :as data]
            [toto.core :as core]
            [hiccup.form :as form]
            [ring.util.response :as ring]))

(def page-title "Toto")

(defn render-page [& contents]
  (html [:html
         [:title page-title]
         [:body contents]
         [:hr]
         [:a { :href "/logout"} "logout"]
         (if (not (nil? core/*username*))
           (str " - " core/*username*)
           "")]))

(defn render-login-page []
  (render-page (form/form-to
                [:post "/login"]
                [:table
                 [:tr [:td { :colspan 2 } [:center "Login to Toto"]]]
                 [:tr [:td "Username:"] [:td (form/text-field {} "username")]]
                 [:tr [:td "Password:"] [:td (form/password-field {} "password")]]
                 [:tr [:td ] [:td (form/submit-button {} "Login")]]])))


(defn add-item [item-description]
  (data/add-todo-item ((data/get-user-by-name core/*username*) :user_id)
                      item-description)
  (ring/redirect "/"))

(defn render-todo-list []
  (render-page [:table
                [:tr [:td "User"] [:td "Description"]]
                (map (fn [item-info]
                       [:tr
                        [:td (item-info :user_id)]
                        [:td (item-info :desc)]])
                     (data/get-pending-items))
                [:tr [:td]
                 [:td (form/form-to [:post "/item"]
                                    (form/text-field {} "item-description"))]]]))

(defn render-users []
  (render-page [:h1 "List of Users"]
               [:ul
                (map (fn [user-name]
                       [:li [:a {:href (str "/user/" user-name)} user-name]])
                     (data/all-user-names))]))

(defn render-user [name]
  (let [user-info (data/get-user-by-name name)]
    (render-page [:h1 (str "User: " (user-info :name))]
                 [:table
                  [:tr [:td "Name"] [:td (user-info :name)]]
                  [:tr [:td "e-mail"] [:td (user-info :email_addr)]]
                  [:tr [:td "password"] [:td (user-info :password)]]])))