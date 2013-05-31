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
                 [:tr [:td "Username:"] [:td (form/text-field {} "username")]]
                 [:tr [:td "Password:"] [:td (form/password-field {} "password")]]
                 [:tr [:td ] [:td (form/submit-button {} "Login")]]])))

(defn render-new-user-form []
  (render-page (form/form-to
                [:post "/user"]
                [:table
                 [:tr [:td { :colspan 2 } [:center "Login to Toto"]]]
                 [:tr [:td "Name:"] [:td (form/text-field {} "username")]]
                 [:tr [:td "E-Mail Address:"] [:td  (form/text-field {} "email_addr")]]
                 [:tr [:td "Password:"] [:td (form/password-field {} "password")]]
                 [:tr [:td ] [:td (form/submit-button {} "Create User")]]])))

(defn add-item [item-description]
  (data/add-todo-item ((data/get-user-by-name core/*username*) :user_id)
                      item-description)
  (ring/redirect "/"))

(defn add-user [username email-addr password] 
  (data/add-user username email-addr (credentials/hash-bcrypt password))
  (ring/redirect "/"))

(defn render-todo-list []
  (render-page [:table
                [:tr [:td "User"] [:td "Description"] [:td]]
                (map (fn [item-info]
                       [:tr
                        [:td (item-info :user_id)]
                        [:td [:a {:href (str "/item/" (item-info :item_id))} (item-info :desc)]]
                        [:td (form/form-to [:post (str "/item/" (item-info :item_id) "/complete")] (form/submit-button {} "Complete"))]])
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

(defn complete-item [item-id]
  (data/complete-item-by-id item-id)
  (ring/redirect "/"))

(defn render-item [id]
  (let [item-info (data/get-item-by-id id)]
    (render-page [:h1 (str "Item: " id)]
                 [:table
                  [:tr [:td "Description:"] [:td (item-info :desc)]]
                  [:tr [:td "Completed:"] [:td (item-info :completed)]]]
                 [:a {:href "/"} "Home"])))

(defn render-user [name]
  (let [user-info (data/get-user-by-name name)]
    (render-page [:h1 (str "User: " (user-info :name))]
                 [:table
                  [:tr [:td "Name"] [:td (user-info :name)]]
                  [:tr [:td "e-mail"] [:td (user-info :email_addr)]]
                  [:tr [:td "password"] [:td (user-info :password)]]])))