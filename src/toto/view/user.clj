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
                 "inactive. A verification e-mail can be sent by following "
                 [:a {:href (str "/user/verify/" (current-user-id))} "this link"]
                 "."]]))

(defn user-password-expired [ request ]
  (render-page { :page-title "Password Expired"}
               [:div.page-message
                [:h1 "Password Expired"]
                [:p
                 "Your password has expired and needs to be reset. "
                 "This can be done at "
                 [:a {:href (str "/user/password")} "this link"]
                 "."]]))

(defn user-account-locked [ request ]
  (render-page { :page-title "Account Locked"}
               [:div.page-message
                [:h1 "Account Locked"]
                [:p
                 "Your account is locked and must be re-verified by e-mail."
                 "An verification e-mail can be sent by following "
                 [:a {:href (str "/user/unlock/" (current-user-id))} "this link"]
                 "."]]))

(defn unauthorized-handler [request]
  (let [roles (auth/current-roles)]
    {:status 403
     :body ((cond
              (:toto.role/unverified roles)
              user-unverified

              (:toto.role/expired-password roles)
              user-password-expired

              (:toto.role/locked-account roles)
              user-account-locked

              :else
              user-unauthorized)
            request)}))

(defn user-create-notification-email-message [ user-email-address ]
  [:body
   [:h1
    "New User Created"]
   [:p
    "New user e-mail: "  user-email-address "."]])

(defn notify-user-create [ config email-addr ]
  (mail/send-email config
                   {:to (:admin-mails config)
                    :subject "Todo - New User Account Created"
                    :content (user-create-notification-email-message email-addr)}))

(defn create-user [ config email-addr password ]
  (let [user-id (auth/create-user email-addr password)
        list-id (data/add-list "Todo")]
    (data/set-list-ownership list-id #{ user-id })
    (notify-user-create config email-addr)
    user-id))

(defn wrap-authenticate [ app ]
  (auth/wrap-authenticate app unauthorized-handler))

(defn render-forgot-password-form []
  (render-page { :page-title "Forgot Password" }
   (form/form-to
    {:class "auth-form"}
    [:post "/user/password-reset"]
    [:p
     "Please enter your e-mail address. If an account is associated with that "
     "address, an e-mail will be sent with a link to reset the password."]
    [:div.config-panel.toplevel
     (form/text-field {:placeholder "E-Mail Address"} "email_addr")]
    [:div.submit-panel
     (form/submit-button {} "Send Reset E-Mail")]))  )

(defn render-login-page [ & { :keys [ email-addr login-failure?]}]
  (render-page { :page-title "Log In" }
   (form/form-to
    {:class "auth-form"}
    [:post "/login"]
    [:div.config-panel.toplevel
     (form/text-field {:placeholder "E-Mail Address"} "username" email-addr)
     (form/password-field {:placeholder "Password"} "password")
     [:div.error-message
      (when login-failure?
        "Invalid username or password.")]]
    [:div.submit-panel
     [:a { :href "/user"} "Register New User"]
     " - "
     [:a { :href "/user/forgot-password"} "Forgot Password"]
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

(defn add-user [ config email-addr password password2 ]
  (cond
   (data/user-email-exists? email-addr)
   (render-new-user-form :error-message "User with this e-mail address already exists.")

   (not (= password password2))
   (render-new-user-form :error-message "Passwords do not match.")

   :else
   (do
     (let [user-id (create-user config email-addr password)]
       (ring/redirect (str "/user/verify/" user-id))))))

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
                    [:a {:href "/user/password"} "Change Password"]]))))

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
                               [:post "/user/password"]

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
                                [:h1 "Change Password"]
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
        ;; new password and assign the user the correct roles for an
        ;; account with a valid password. (This is needed so that we
        ;; allow the user use the website, if their password had
        ;; expired.)
        (log/warn "Password change unexpectedly fell through workflow!")
        (ring/redirect "/")))))

(defn- get-link-verified-user [ link-user-id link-uuid ]
  (when-let [ user-id (:verifies_user_id (data/get-verification-link-by-uuid link-uuid)) ]
    (when (= link-user-id user-id)
      (data/get-user-by-id user-id))))

(defn render-password-reset-form [ link-user-id link-uuid error-message ]
  (when-let [ user (get-link-verified-user link-user-id link-uuid)]
    (render-page { :page-title "Reset Password" }
                 [:div.page-message
                  [:h1 "Reset Password"]
                  [:div.config-panel
                   (form/form-to {:class "auth-form"}
                                 [:post (str "/user/password-reset/" (:user_id user))]

                                 [:h1 "Password"]
                                 [:input {:type "hidden"
                                          :name "link_uuid"
                                          :value link-uuid}]
                                 (form/password-field {:placeholder "New Password"} "new_password1")
                                 (form/password-field {:placeholder "Verify Password"} "new_password2")
                                 [:div#error.error-message
                                  error-message]
                                 (form/submit-button {} "Reset Password"))]])))

(defn password-reset [ user-id link-uuid new-password-1 new-password-2 ]
  (let [ user (get-link-verified-user user-id link-uuid)]
    (cond
      (not user)
      nil

      (not (= new-password-1 new-password-2))
      (render-password-reset-form user-id link-uuid "Passwords do not match.")

      :else
      (do
        (auth/set-user-password (:email_addr user) new-password-1)
        ;; Resetting the password via a link also serves to
        ;; unlock the account.
        (data/reset-login-failures (:user_id user))
        (ring/redirect "/user/password-reset-success")))))

(defn- ensure-verification-link [ user-id ]
  (unless (data/get-verification-link-by-user-id user-id)
    (data/create-verification-link user-id)))

(defn- development-verification-form [ user-id ]
  [:div.dev-tool
   (let [ link-uuid (:link_uuid (data/get-verification-link-by-user-id user-id))]
     [:a {:href (str "/user/verify/" user-id "/" link-uuid)} "Verify"])])

(defn- development-unlock-form [ user-id ]
  [:div.dev-tool
   (let [ link-uuid (:link_uuid (data/get-verification-link-by-user-id user-id))]
     [:a {:href (str "/user/unlock/" user-id "/" link-uuid)} "Unlock"])])

(defn- development-reset-form [ user-id ]
  [:div.dev-tool
   (let [ link-uuid (:link_uuid (data/get-verification-link-by-user-id user-id))]
     [:a {:href (str "/user/reset/" user-id "/" link-uuid)} "Reset"])])

(defn- development-no-user-form []
  [:div.dev-tool
   "No user with this e-mail address exists"])

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
  (when-let [ user (get-link-verified-user link-user-id link-uuid ) ]
    (data/add-user-roles (:user_id user) #{:toto.role/verified})
    (render-page { :page-title "e-Mail Address Verified" }
                 [:div.page-message
                  [:h1 "e-Mail Address Verified"]
                  [:p "Thank you for verifying your e-mail address at: "
                   [:span.addr (:email_addr user)] ". Using the link below, you "
                   "can log in and start to use the system."]
                  [:a {:href "/"} "Login"]])))

(defn enter-unlock-workflow [ config user-id ]
  (let [user (data/get-user-by-email (current-identity))]
    (ensure-verification-link user-id)
    (send-verification-link config user-id)
    (render-page { :page-title "Unlock Account" }
                      [:div.page-message
                       [:h1 "Unlock Account"]
                       [:p "An e-mail has been sent to "  [:span.addr (:email_addr user)]
                        " with a link you may use to unlock your account. Please"
                        " check your spam filter if it does not appear within a few minutes."]
                       [:a {:href "/"} "Login"]
                       (when (:development-mode config)
                         (development-unlock-form user-id))])))

(defn unlock-user [ link-user-id link-uuid ]
  (when-let [ user (get-link-verified-user link-user-id link-uuid ) ]
    (data/reset-login-failures (:user_id user))
    (render-page { :page-title "Account Unlocked" }
                 [:div.page-message
                  [:h1 "Account Unlocked"]
                  [:p "Thank you for unlocking your account at: "
                   [:span.addr (:email_addr user)] ". Using the link below, you "
                   "can log in and start to use the system."]
                  [:a {:href "/"} "Login"]])))

(defn enter-password-reset-workflow [ config email-addr ]
  (let [user (data/get-user-by-email email-addr)
        user-id (and user (:user_id user))]
    (when user-id
      (ensure-verification-link user-id)
      (send-verification-link config user-id))
    (render-page { :page-title "Reset Password" }
                 [:div.page-message
                  [:h1 "Reset Password"]
                  [:p "If there is an account with this e-mail address, an e-mail"
                   " has been sent with a link you may use to reset your password. Please"
                   " check your spam filter if it does not appear within a few minutes."]
                  [:a {:href "/"} "Login"]
                  (when (:development-mode config)
                    (if user-id
                      (development-reset-form user-id)
                      (development-no-user-form)))])))

(defn render-password-reset-success []
    (render-page { :page-title "Password Successfully Reset" }
                 [:div.page-message
                  [:h1 "Password Successfully Reset"]
                  [:p "Your password has been reset. You can login "
                   [:a {:href "/"} "here"] "."]])  )

(defn private-routes [ config ]
  (routes
   (GET "/user/password" []
     (render-change-password-form))

   (POST "/user/password" {params :params}
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
     (add-user config (:email_addr params) (:password1 params) (:password2 params)))

   (GET "/login" { { login-failed :login_failed email-addr :username } :params }
     (render-login-page :email-addr email-addr
                        :login-failure? (= login-failed "Y")))

   ;; User Verification Workflow
   (GET "/user/verify/:user-id" { { user-id :user-id } :params }
     (enter-verify-workflow config user-id))

   (friend/logout
    (GET "/user/verify/:user-id/:link-uuid" { { user-id :user-id link-uuid :link-uuid } :params }
      (verify-user (parsable-integer? user-id) link-uuid)))

   ;; Account Unlock workflow
   (GET "/user/unlock/:user-id" { { user-id :user-id } :params }
     (enter-unlock-workflow config user-id))

   (friend/logout
    (GET "/user/unlock/:user-id/:link-uuid" { { user-id :user-id link-uuid :link-uuid } :params }
      (unlock-user (parsable-integer? user-id) link-uuid)))

   ;; Password Reset Workflow
   (GET "/user/forgot-password" []
     (render-forgot-password-form))

   (POST "/user/password-reset" { { email-addr :email_addr } :params }
     (enter-password-reset-workflow config email-addr))

   (POST "/user/password-reset/:user-id" {params :params}
     (password-reset (parsable-integer? (:user-id params)) (:link_uuid params) (:new_password1 params) (:new_password2 params)))

   (friend/logout
    (GET "/user/reset/:user-id/:link-uuid" { { user-id :user-id link-uuid :link-uuid error-message :error-message } :params }
      (render-password-reset-form (parsable-integer? user-id) link-uuid error-message)))

   (GET "/user/password-reset-success" []
     (render-password-reset-success))

   ;; Logout Link
   (friend/logout
    (ANY "/logout" [] (ring/redirect "/")))

   ;; Secure Links
   (wrap-routes (private-routes config)
                friend/wrap-authorize
                #{:toto.role/verified})))
