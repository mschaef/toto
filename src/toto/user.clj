(ns toto.user
  (:use toto.util
        compojure.core)
  (:require [cemerick.friend.credentials :as credentials]
            [ring.util.response :as ring]
            [cemerick.friend :as friend]
            [hiccup.form :as form]
            [toto.core :as core]
            [toto.data :as data]
            [toto.view :as view]))

(defn render-login-page [ & { :keys [ email-addr login-failure?]}]
  (view/render-page { :page-title "Log In" }
   (form/form-to
    [:post "/login"]
    [:table { :class "form" }
     (table-row "E-Mail Address:" (form/text-field { :class "simple-border" } "username" (if email-addr email-addr)))
     (table-row "Password:" (form/password-field { :class "simple-border" } "password"))
     (if login-failure?
       [:tr [:td { :colspan 4 } [:div#error "Invalid username or password."]]])
     [:tr 
      [:td { :colspan 4 }
       [:center
        [:a { :href "/user"} "Register New User"]
        " - "
        (form/submit-button {} "Login")]]]])))

(defn render-new-user-form [ & { :keys [ error-message ]}]
  (view/render-page {:page-title "New User Registration"
                     :include-js [ "/toto-new-user.js" ]}
   (form/form-to
    [:post "/user"]
    [:table { :class "form" }
     (table-row "E-Mail Address:" (form/text-field { :class "simple-border" } "email_addr"))
     (table-row "Password:" (form/password-field { :class "simple-border" } "password1"))
     (table-row "Verify Password:" (form/password-field { :class "simple-border" } "password2"))
     (table-row "&nbsp;" [:div#error error-message])
     (table-row "" (form/submit-button {} "Register"))])))

(defn create-user  [ email-addr password ]
  (let [uid (data/add-user email-addr password)
        list-id (data/add-list "Todo")]
    (data/add-list-owner uid list-id)
    uid))

(defn add-user [ email-addr password password2 ] 
  (cond
   (data/user-email-exists? email-addr)
   (render-new-user-form :error-message "User with this e-mail address already exists.")

   (not (= password password2))
   (render-new-user-form :error-message "Passwords do not match.")

   :else
   (do
     (create-user email-addr (credentials/hash-bcrypt password))
     (ring/redirect "/"))))

(defroutes public-routes
  (GET "/user" []
       (render-new-user-form))

  (POST "/user" {{email-addr :email_addr password1 :password1 password2 :password2} :params}
        (add-user email-addr password1 password2))

  (friend/logout (ANY "/logout" []  (ring.util.response/redirect "/")))

  (GET "/login" { { login-failed :login_failed email-addr :username } :params }
       (render-login-page :email-addr email-addr
                          :login-failure? (= login-failed "Y"))))

