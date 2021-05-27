(ns toto.view.user
  (:use toto.core.util
        compojure.core
        hiccup.core
        toto.view.common)
  (:require [clojure.tools.logging :as log]
            [ring.util.response :as ring]
            [cemerick.friend :as friend]
            [hiccup.form :as form]
            [toto.core.mail :as mail]
            [toto.data.data :as data]
            [toto.view.auth :as auth]))

(defn user-unauthorized [ request ]
  (render-page { :page-title "Access Denied"}
               [:div.page-message
                [:h1 "Access Denied"]]))

(defn user-unverified [ request ]
  (render-page { :page-title "E-Mail Unverified"}
               [:div.page-message
                [:h1 "E-Mail Unverified"]
                [:p
                 "Your e-mail address is unverified and your acccount is "
                 "inactive. An verification e-mail can be sent by following "
                 [:a {:href (str "/user/begin-verify/" (current-user-id))} "this link"]
                 "."]]))

(defn user-password-expired [ request ]
  (render-page { :page-title "Password Expired"}
               [:div.page-message
                [:h1 "Password Expired"]
                [:p
                 "Your password has expired and needs to be reset. "
                 "This can be done at "
                 [:a {:href (str "/user/password-change")} "this link"]
                 "."]]))

(defn missing-verification? [ request ]
  (= (auth/request-missing-roles request)
     #{:toto.role/verified}))

(defn missing-current-password? [ request ]
  (= (auth/request-missing-roles request)
     #{:toto.role/current-password}))

(defn unauthorized-handler [request]
  {:status 403
   :body ((cond
            (missing-verification? request)
            user-unverified

            (missing-current-password? request)
            user-password-expired

            :else
            user-unauthorized)
          request)})

(defn create-user [ email-addr password ]
  (let [user-id (auth/create-user email-addr password)
        list-id (data/add-list "Todo")]
    (data/set-list-ownership list-id #{ user-id })
    user-id))

(defn wrap-authenticate [ request ]
  (auth/wrap-authenticate request unauthorized-handler))

(defn render-login-page [ & { :keys [ email-addr login-failure?]}]
  (render-page { :page-title "Log In" }
   (form/form-to
    {:class "auth-form"}
    [:post "/login"]
    [:div.config-panel
     (form/text-field {:placeholder "E-Mail Address"} "username" email-addr)
     (form/password-field {:placeholder "Password"} "password")
     [:div.error-message
      (when login-failure?
        "Invalid username or password.")]]
    [:div.submit-panel
     [:a { :href "/user"} "Register New User"]
     " - "
     (form/submit-button {} "Login")])))

(defn render-new-user-form [ & { :keys [ error-message ]}]
  (render-page {:page-title "New User Registration"
                     :page-data-class "init-new-user"}
   (form/form-to
    {:class "auth-form"}
    [:post "/user"]
    [:div.config-panel
     [:h1 "Identity"]
     (form/text-field {:placeholder "E-Mail Address"} "email_addr")]
    [:div.config-panel
     [:h1 "Password"]
     (form/password-field {:placeholder "Password"} "password1")
     (form/password-field {:placeholder "Verify Password"} "password2")
     [:div#error.error-message
      error-message]]
    [:div.submit-panel
     (form/submit-button {} "Register")])))

(defn verification-email-message [ config verify-link-url ]
  [:body
   [:h1
    "Verification E-mail"]
   [:p
    "Thank you for registering with " [:a {:href (:base-url config)} "Toto"]
    " to manage your todo lists. You can verify your e-mail address by clicking"
    [:a {:href verify-link-url} "here"] "."]

   [:p
    "If this isn't something you've requested, you can safely ignore this"
    " e-mail, and we won't send anything else."]])


(defn send-verification-link [ config user-id ]
  (let [user (data/get-user-by-id user-id)
        verification-link (data/get-verification-link-by-user-id user-id)]
    (let [link-url (str (:base-url config) "user/verify/" user-id "/"
                        (:link_uuid verification-link))]
      (mail/send-email config
                       {:to [ (:email_addr user) ]
                        :subject "Todo - Confirm E-Mail Address"
                        :content (verification-email-message config link-url)}))))

(defn add-user [ email-addr password password2 ]
  (cond
   (data/user-email-exists? email-addr)
   (render-new-user-form :error-message "User with this e-mail address already exists.")

   (not (= password password2))
   (render-new-user-form :error-message "Passwords do not match.")

   :else
   (do
     (let [user-id (create-user email-addr password)]
       (ring/redirect (str "/user/begin-verify/" user-id))))))

(def date-format (java.text.SimpleDateFormat. "yyyy-MM-dd hh:mm aa"))

(defn render-user-info-form [ & { :keys [ error-message ]}]
  (let [user (data/get-user-by-email (current-identity))]
    (render-page { :page-title "User Information" }
                 (form/form-to
                  [:post "/user/info"]
                  [:input {:type "hidden"
                           :name "username"
                           :value (current-identity)}]
                  
                  
                  [:div.config-panel
                   [:h1 "E-Mail Address"]
                   (current-identity)]

                  [:div.config-panel
                   [:h1 "Name"]
                   [:div
                    
                    [:input {:name "name"
                             :type "text"
                             :value (:friendly_name user)}]
                    (form/submit-button {} "Update")]
                   (when error-message
                     [:div.error-message error-message])]

                  [:div.config-panel
                   [:h1 "Last Login"]
                   (.format date-format (or (:last_login_on user) (java.util.Date.)))]

                  [:div.config-panel
                    [:a {:href "/user/password-change"} "Change Password"]]))))

(defn validate-name [ name ]
  (if name
    (let [ name (.trim name )]
      (and (> (.length name) 0)
           (< (.length name) 32)
           name))))

(defn update-user-info [ name ]
  (if-let [name (validate-name name)]
    (do
      (data/set-user-name (current-identity) name)
      (ring/redirect "/user/info"))
    (render-user-info-form :error-message "Invalid Name")))

(defn render-change-password-form  [ & { :keys [ error-message ]}]
  (let [user (data/get-user-by-email (current-identity))]
    (render-page { :page-title "Change Password" }
                 (form/form-to {:class "auth-form"}
                               [:post "/user/password-change"]

                               [:input {:type "hidden"
                                        :name "username"
                                        :value (current-identity)}]
                               
                               [:div.config-panel
                                [:h1 "E-Mail Address"]
                                (current-identity)]

                               [:div.config-panel
                                [:h1 "Name"]
                                (:friendly_name user)]

                               [:div.config-panel
                                [:h1 "Last Login"]
                                (.format date-format (or (:last_login_on user) (java.util.Date.)))]

                               [:div.config-panel
                                [:h1 "Reset Password"]
                                (form/password-field {:placeholder "Password"} "password")
                                (form/password-field {:placeholder "New Password"} "new_password1")
                                (form/password-field {:placeholder "Verify Password"} "new_password2")
                                (when error-message
                                  [:div.error-message error-message])
                                [:div
                                 (form/submit-button {} "Change Password")]]))))

(defn change-password [ password new-password-1 new-password-2 ]
  (let [ username (current-identity) ]
    (cond
      (not (auth/get-user-by-credentials {:username username :password password}))
      (render-change-password-form
       :error-message "Old password incorrect.")

      (= password new-password-2)
      (render-change-password-form
       :error-message "New password cannot be the same as the old.")

      (not (= new-password-1 new-password-2))
      (render-change-password-form
       :error-message "Passwords do not match.")

      :else
      (do
        ;; The password change handling is done in a Friend workflow
        ;; handler, so that it can reauthenticate the user against the
        ;; new password and assign the user the current-password role
        ;; if it was previously missing. (This is needed so that we
        ;; allow the user use the website, if their password had
        ;; expired.)
        (log/warn "Password change unexpectedly fell through workflow!")
        (ring/redirect "/")))))

(defn ensure-verification-link [ user-id ]
  (unless (data/get-verification-link-by-user-id user-id)
    (data/create-verification-link user-id)))

(defn development-verification-form [ user-id ]
  [:div.dev-tool
   (let [ link-uuid (:link_uuid (data/get-verification-link-by-user-id user-id))]
     [:a {:href (str "/user/verify/" user-id "/" link-uuid)} "Verify"])])

(defn enter-verify-workflow [ config user-id ]
  (let [ user (data/get-user-by-id user-id) ]
    (ensure-verification-link user-id)
    (send-verification-link config user-id)
    (render-page { :page-title "e-Mail Address Verification" }
                      [:div.page-message
                       [:h1 "e-Mail Address Verification"]
                       [:p "An e-mail has been sent to "  [:span.addr (:email_addr user)]
                        " with a link you may use to verify your e-mail address. Please"
                        " check your spam filter if it does not appear within a few minutes."]
                       [:a {:href "/"} "Login"]
                       (when (:development-mode config)
                         (development-verification-form user-id))])))

(defn verify-user [ link-user-id link-uuid ]
  (log/error "verify user " [ link-user-id link-uuid ])
  (when-let [ user-id (:verifies_user_id (data/get-verification-link-by-uuid link-uuid)) ]
    (let [ email-addr (:email_addr (data/get-user-by-id user-id)) ]
      (data/add-user-roles user-id #{:toto.role/verified})
      (render-page { :page-title "e-Mail Address Verified" }
                        [:div.page-message
                         [:h1 "e-Mail Address Verified"]
                         [:p "Thank you for verifying your e-mail address at: "
                          [:span.addr email-addr] ". Using the link below, you "
                          "can log in and start to use the system."]
                         [:a {:href "/"} "Login"]]))))

(defn private-routes [ config ]
  (routes
   (GET "/user/password-change" []
     (render-change-password-form))

   (POST "/user/password-change" {params :params}
     (change-password (:password params) (:new_password1 params) (:new_password2 params)))

   (GET "/user/info" []
     (render-user-info-form))

   (POST "/user/info" { { name :name } :params }
     (update-user-info name))))

(defn all-routes [ config ]
  (routes
   (GET "/user" []
     (render-new-user-form))

   (POST "/user" {params :params}
     (add-user (:email_addr params) (:password1 params) (:password2 params)))


   (GET "/login" { { login-failed :login_failed email-addr :username } :params }
     (render-login-page :email-addr email-addr
                        :login-failure? (= login-failed "Y")))

   (GET "/user/begin-verify/:user-id" { { user-id :user-id } :params }
     (enter-verify-workflow config user-id))

   (friend/logout
    (GET "/user/verify/:user-id/:link-uuid" { { user-id :user-id link-uuid :link-uuid } :params }
      (verify-user user-id link-uuid)))

   (friend/logout
    (ANY "/logout" [] (ring/redirect "/")))

   (wrap-routes (private-routes config)
                friend/wrap-authorize
                #{:toto.role/verified})))
