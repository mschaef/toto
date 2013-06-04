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

(defn render-new-user-form []
  (render-page (form/form-to
                [:post "/user"]
                [:table
                 [:tr [:td { :colspan 2 } [:center "Create New User"]]]
                 [:tr [:td "E-Mail Address:"] [:td  (form/text-field {} "email_addr")]]
                 [:tr [:td "Password:"] [:td (form/password-field {} "password")]]
                 [:tr [:td ] [:td (form/submit-button {} "Create User")]]])))


(defn add-user [email-addr password] 
  (data/create-user email-addr (credentials/hash-bcrypt password))
  (ring/redirect "/"))

(defn render-todo-list []
  (render-page [:table
                [:tr [:td "User"] [:td "Description"] [:td]]
                (map (fn [item-info]
                       [:tr
                        [:td (item-info :todo_list_id)]
                        [:td [:a {:href (str "/item/" (item-info :item_id))} (item-info :desc)]]
                        [:td (form/form-to [:post (str "/item/" (item-info :item_id) "/complete")] (form/submit-button {} "Complete"))]])
                     (data/get-pending-items))
                [:tr [:td]
                 [:td (form/form-to [:post "/item"]
                                    (form/text-field {} "item-description"))]]]))

(defn render-users []
  (render-page [:h1 "List of Users"]
               [:ul
                (map (fn [user]
                       [:li [:a {:href (str "/user/" (user :user_id))} (user :email_addr)]])
                     (data/all-users))]))

(defn current-todo-list-id []
  (first (data/get-todo-list-ids-by-user ((data/get-user-by-email core/*username*) :user_id))))

(defn add-item [item-description]
  (data/add-todo-item (current-todo-list-id)
                      item-description)
  (ring/redirect "/"))

(defn update-item [item-id item-description]
  (data/update-item-by-id item-id item-description)
  (ring/redirect "/"))

(defn complete-item [item-id]
  (data/complete-item-by-id item-id)
  (ring/redirect "/"))

(defn render-item [id]
  (let [item-info (data/get-item-by-id id)]
    (render-page [:h1 (str "Item: " id)]
                 (form/form-to [:post (str "/item/" id)]
                               [:table
                                [:tr [:td "Description:"] [:td (form/text-field {} "description" (item-info :desc))]]
                                [:tr [:td "Completed:"] [:td (item-info :completed)]]]
                               (form/submit-button {} "Update Item"))
                 [:a {:href "/"} "Home"])))

(defn render-user [id]
  (let [user-info (data/get-user-by-id id)]
    (render-page [:h1 (str "User: " (user-info :name))]
                 [:table
                  [:tr [:td "Name"] [:td (user-info :name)]]
                  [:tr [:td "e-mail"] [:td (user-info :email_addr)]]
                  [:tr [:td "password"] [:td (user-info :password)]]])))