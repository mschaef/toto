(ns toto.user
  (:use toto.util
        compojure.core)
  (:require [clojure.tools.logging :as log]
            [cemerick.friend.credentials :as credentials]
            [ring.util.response :as ring]
            [cemerick.friend :as friend]
            [hiccup.form :as form]
            [toto.core :as core]
            [toto.data :as data]
            [toto.view :as view]))

(defn get-user-by-credentials [ creds ]
  (if-let [user-record (data/get-user-by-email (creds :username))]
    (and (credentials/bcrypt-verify (creds :password) (user-record :password))
         {:identity (creds :username)
          :roles  (data/get-user-roles (:user_id user-record))})
    nil))

(defn render-login-page [ & { :keys [ email-addr login-failure?]}]
  (view/render-page { :page-title "Log In" }
   (form/form-to
    [:post "/login"]
    [:table { :class "form" }
     (table-row "E-Mail Address:" (form/text-field { :class "simple-border" } "username" (if email-addr email-addr)))
     (table-row "Password:" (form/password-field { :class "simple-border" } "password"))
     (if login-failure?
       [:tr [:td { :colspan 4 } [:div.error-message "Invalid username or password."]]])
     [:tr 
      [:td { :colspan 4 }
       [:center
        [:a { :href "/user"} "Register New User"]
        " - "
        (form/submit-button {} "Login")]]]])))

(defn render-new-user-form [ & { :keys [ error-message ]}]
  (view/render-page {:page-title "New User Registration"
                     :init-map { :page "new-user" }}
   (form/form-to
    [:post "/user"]
    [:table { :class "form" }
     (table-row "E-Mail Address:" (form/text-field { :class "simple-border" } "email_addr"))
     (table-row "Password:" (form/password-field { :class "simple-border" } "password1"))
     (table-row "Verify Password:" (form/password-field { :class "simple-border" } "password2"))
     (table-row "&nbsp;" [:div#error error-message])
     (table-row "" (form/submit-button {} "Register"))])))

(defn create-user  [ email-addr password ]
  (let [user-id (data/add-user email-addr password)
        list-id (data/add-list "Todo")]
    (data/add-list-owner user-id list-id)
    user-id))

(defn add-user [ email-addr password password2 ] 
  (cond
   (data/user-email-exists? email-addr)
   (render-new-user-form :error-message "User with this e-mail address already exists.")

   (not (= password password2))
   (render-new-user-form :error-message "Passwords do not match.")
 
   :else
   (do
     (let [user-id (create-user email-addr (credentials/hash-bcrypt password))
           link-id (data/create-verification-link user-id)]
       (log/spy :error (data/get-verification-link-by-id link-id))
       (ring/redirect "/")))))

(defn render-change-password-form  [ & { :keys [ error-message ]}]
  (view/render-page { :page-title "Change Password" }
   (form/form-to
    [:post "/user/password"]
    [:table { :class "form" }
     (table-row "E-Mail Address:" (core/authenticated-username))
     (table-row "Old Password:" (form/password-field "password"))
     (table-row "New Password:" (form/password-field "new_password1"))
     (table-row "Verify Password:" (form/password-field "new_password2"))
     
     (unless (empty? error-message)
       [:tr [:td { :colspan 2 } [:div#error error-message]]])
     
     [:tr [:td ] [:td (form/submit-button {} "Change Password")]]]))  )

(defn change-password [ password new-password-1 new-password-2 ]
  (let [ username (core/authenticated-username) ]
    (cond
      (not (get-user-by-credentials {:username username :password password}))
      (render-change-password-form :error-message "Old Password Incorrect")

      (not (= new-password-1 new-password-2))
      (render-change-password-form :error-message "Passwords do not match.")

      :else
      (do
        (log/info "Changing Password for user:" username)
        (data/set-user-password username (credentials/hash-bcrypt new-password-1))
        (ring/redirect "/")))))

(defn verify-user [ link-uuid ]
  (when-let [ user-id (:verifies_user_id (data/get-verification-link-by-uuid link-uuid)) ]
    (let [ email-addr (:email_addr (data/get-user-by-id user-id)) ]
      (data/set-user-roles user-id
                           (clojure.set/union (data/get-user-roles user-id)
                                              #{:petros.role/verified}))
      (view/render-page { :page-title "e-Mail Address Verified" }
                        [:h1 "e-Mail Address Verified"]
                        [:p "Thank you for verifying your e-mail address at: "
                         [:span.addr email-addr] ". Using the link below, you "
                         "can log in and start to use the system."]
                        [:a {:href "/"} "Login"]))))

(defroutes public-routes
  (GET "/user" []
       (render-new-user-form))

  (POST "/user" {{email-addr :email_addr password1 :password1 password2 :password2} :params}
        (add-user email-addr password1 password2))


  (GET "/login" { { login-failed :login_failed email-addr :username } :params }
       (render-login-page :email-addr email-addr
                          :login-failure? (= login-failed "Y")))

  (friend/logout
   (GET "/user/verify/:uuid" { { link-uuid :uuid } :params }
     (verify-user link-uuid)))
  
  (friend/logout
   (ANY "/logout" [] (ring/redirect "/"))))


(defroutes private-routes
  (GET "/user/password" []
    (render-change-password-form))

  (POST "/user/password" {{password :password new-password-1 :new_password1 new-password-2 :new_password2} :params}
    (change-password password new-password-1 new-password-2)))
