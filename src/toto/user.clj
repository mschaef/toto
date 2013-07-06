(ns toto.user
  (:use compojure.core)
  (:require [cemerick.friend.credentials :as credentials]
            [ring.util.response :as ring]
            [hiccup.form :as form]
            [toto.data :as data]
            [toto.view :as view]))

(defn render-new-user-form [ & { :keys [ error-message ]}]
  (view/render-page (form/form-to
                [:post "/user"]
                [:table
                 [:tr [:td { :colspan 2 } [:center "Create New User"]]]
                 [:tr [:td "E-Mail Address:"] [:td  (form/text-field {} "email_addr")]]
                 [:tr [:td "Password:"] [:td (form/password-field {} "password")]]
                 [:tr [:td "Verify Password:"] [:td (form/password-field {} "password2")]]

                 (if (not (empty? error-message))
                   [:tr [:td { :colspan 2 } [:div#error error-message]]])

                 [:tr [:td ] [:td (form/submit-button {} "Create User")]]])))

(defn add-user [ email-addr password password2 ] 
  (cond
   (data/user-email-exists? email-addr)
   (render-new-user-form :error-message "User with this e-mail address already exists.")

   (not (= password password2))
   (render-new-user-form :error-message "Passwords do not match.")

   :else
   (do
     (data/create-user email-addr (credentials/hash-bcrypt password))
     (ring/redirect "/"))))

(defn render-users []
  (view/render-page [:h1 "List of Users"]
               [:ul
                (map (fn [user]
                       [:li [:a {:href (str "/user/" (user :user_id))} (user :email_addr)]])
                     (data/all-users))]))

(defn render-user [id]
  (let [user-info (data/get-user-by-id id)]
    (view/render-page [:h1 (str "User: " (user-info :name))]
                 [:table
                  [:tr [:td "Name"] [:td (user-info :name)]]
                  [:tr [:td "e-mail"] [:td (user-info :email_addr)]]
                  [:tr [:td "password"] [:td (user-info :password)]]])))

(defroutes public-routes
  (GET "/user" []
       (render-new-user-form))

  (POST "/user" {{email-addr :email_addr password :password password2 :password2} :params}
        (add-user email-addr password password2)))

(defroutes admin-routes
  (GET "/users" []
       (render-users))

  (GET "/user/:id" [id]
       (render-user id)))
