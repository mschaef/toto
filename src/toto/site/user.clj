;; Copyright (c) 2015-2025 Michael Schaeffer (dba East Coast Toolworks)
;;
;; Licensed as below.
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;       http://www.apache.org/licenses/LICENSE-2.0
;;
;; The license is also includes at the root of the project in the file
;; LICENSE.
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.
;;
;; You must not remove this notice, or any other, from this software.

(ns toto.site.user
  (:use playbook.core
        compojure.core
        hiccup.core
        toto.view.common
        toto.view.components
        toto.view.query
        toto.view.page)
  (:require [taoensso.timbre :as log]
            [ring.util.response :as ring]
            [cemerick.friend :as friend]
            [hiccup.form :as form]
            [playbook.config :as config]
            [toto.site.mail :as mail]
            [toto.data.data :as core-data]
            [toto.todo.data.data :as data]
            [toto.view.auth :as auth]))

(defn user-unauthorized [request]
  (render-page {:title "Access Denied"}
               [:div.page-message
                [:h1 "Access Denied"]]))

(defn user-unverified [request]
  (render-page {:title "E-Mail Unverified"}
               [:div.page-message
                [:h1 "E-Mail Unverified"]
                [:p
                 "Your e-mail address is unverified and your acccount is "
                 "inactive. A verification e-mail can be sent by following "
                 [:a {:href (str "/user/verify/" (auth/current-user-id))} " this link"]
                 "."]]))

(defn user-password-expired [request]
  (render-page {:title "Password Expired"}
               [:div.page-message
                [:h1 "Password Expired"]
                [:p
                 "Your password has expired and needs to be reset. "
                 "This can be done at "
                 [:a {:href (str "/user/password")} " this link"]
                 "."]]))

(defn user-account-locked [request]
  (render-page {:title "Account Locked"}
               [:div.page-message
                [:h1 "Account Locked"]
                [:p
                 "Your account is locked and must be re-verified by e-mail."
                 "An verification e-mail can be sent by following "
                 [:a {:href (str "/user/unlock/" (auth/current-user-id))} " this link"]
                 "."]]))

(defn unauthorized-handler [request]
  (let [roles (auth/current-roles)]
    {:status 403
     :body ((cond
              (:role/unverified roles)
              user-unverified

              (:role/expired-password roles)
              user-password-expired

              (:role/locked-account roles)
              user-account-locked

              :else
              user-unauthorized)
            request)}))

(defn- user-create-notification-message [params]
  [:body
   [:h1
    "New User Created"]
   [:p
    "New user e-mail: " (:email-addr params) "."]])

(defn- send-user-create-notification [email-addr]
  (mail/send-email
   {:to (config/cval :admin-mails)
    :subject "New User Account Created"
    :content user-create-notification-message
    :params {:email-addr email-addr}}))

(defn create-user [friendly-name email-addr password]
  (let [user-id (auth/create-user (.trim friendly-name) email-addr password)
        list-id (data/add-list "Todo" false)]
    (data/set-list-ownership list-id #{user-id})
    (send-user-create-notification email-addr)
    user-id))

(defn wrap-authenticate [app]
  (auth/wrap-authenticate app unauthorized-handler))

(defn render-forgot-password-form []
  (render-page {:title "Forgot Password"}
               (form/form-to
                {:class "auth-form"
                 :data-turbo "false"}
                [:post "/user/password-reset"]
                [:p
                 "Please enter your e-mail address. If an account is associated with that "
                 "address, an e-mail will be sent with a link to reset the password."]
                [:div.config-panel.toplevel
                 (form/text-field {:placeholder "E-Mail Address"} "email-addr")]
                [:div.submit-panel
                 (form/submit-button {} "Send Reset E-Mail")])))

(defn render-login-page [& {:keys [email-addr login-failure?]}]
  (render-page {:title "Log In"}
               (form/form-to
                {:class "auth-form"
                 :data-turbo "false"}
                [:post "/login"]
                [:div.config-panel.toplevel
                 (form/text-field {:placeholder "E-Mail Address"} "username" email-addr)
                 (form/password-field {:placeholder "Password"} "password")
                 [:div.error-message
                  (when login-failure?
                    "Invalid username or password.")]]
                [:div.submit-panel
                 [:a {:href "/user"} "Register New User"]
                 " - "
                 [:a {:href "/user/forgot-password"} "Forgot Password"]
                 " - "
                 (form/submit-button {} "Login")])))

(defn render-new-user-form [& {:keys [error-message]}]
  (render-page
   {:title "New User Registration"
    :page-data-class "init-new-user"}
   (form/form-to
    {:class "auth-form"
     :data-turbo "false"}
    [:post "/user"]
    [:div.config-panel
     [:h1 "Full Name"]
     (form/text-field {:placeholder "Full Name"} "friendly-name")]
    [:div.config-panel
     [:h1 "E-Mail Address"]
     (form/text-field {:placeholder "E-Mail Address"} "email-addr")
     (form/text-field {:placeholder "Verify E-Mail Address"} "email-addr-2")]
    [:div.config-panel
     [:h1 "Password"]
     (form/password-field {:placeholder "Password"} "password")
     (form/password-field {:placeholder "Verify Password"} "password-2")]
    (render-verify-question)
    [:div.submit-panel
     [:div#error.error-message
      error-message]
     (form/submit-button {} "Register")])))

(defn get-verification-link-by-user-id [link-type user-id]
  (let [verification-link (core-data/get-verification-link-by-user-id user-id)]
    (str (config/cval :base-url) "user/" link-type "/" user-id "/"
         (:link_uuid verification-link))))

(defn- verification-link-email-message [params]
  [:body
   [:h1
    "Verification E-mail"]
   [:p
    "Thank you for registering with " [:a {:href (:base-url params)} "Toto"]
    ", the family to-do list manager. You can verify your e-mail address by clicking"
    [:a {:href (:verify-link-url params)} " here"] "."]
   [:p
    "If this isn't something you've requested, you can safely ignore this"
    " e-mail, and we won't send anything else."]])

(defn- send-verification-link [user-id]
  (let [user (core-data/get-user-by-id user-id)
        link-url (get-verification-link-by-user-id "verify" user-id)]
    (mail/send-email {:to [(:email_addr user)]
                      :subject "Verify Account"
                      :content verification-link-email-message
                      :params {:verify-link-url link-url}})))

(defn- unlock-link-email-message [params]
  [:body
   [:h1
    "Unlock Password"]
   [:p
    "Click " [:a {:href (:verify-link-url params)} "here"] " to unlock your "
    "account at " [:a {:href (:base-url params)} "Toto"] ", the family "
    "to-do list manager."]])

(defn- send-unlock-link [user-id]
  (let [user (core-data/get-user-by-id user-id)
        link-url (get-verification-link-by-user-id "unlock" user-id)]
    (mail/send-email {:to [(:email_addr user)]
                      :subject "Unlock Account"
                      :content unlock-link-email-message
                      :params {:verify-link-url link-url}})))

(defn- reset-link-email-message [params]
  [:body
   [:h1
    "Reset Password"]
   [:p
    "Click " [:a {:href (:verify-link-url params)} "here"]
    " to reset your password at " [:a {:href (:base-url params)} "Toto"]
    ", the family to-do list manager."]])

(defn- send-reset-link [user-id]
  (let [user (core-data/get-user-by-id user-id)
        link-url (get-verification-link-by-user-id "reset" user-id)]
    (mail/send-email {:to [(:email_addr user)]
                      :subject "Reset Account Password"
                      :content reset-link-email-message
                      :params {:verify-link-url link-url}})))

(defn add-user [params]
  (let [{:keys [:friendly-name :email-addr :email-addr-2 :password :password-2]} params]
    (cond
      (not (verify-response-correct params))
      (render-new-user-form :error-message "Math problem answer incorrect.")

      (not (parsable-string? friendly-name))
      (render-new-user-form :error-message "No full name specified.")

      (not (= email-addr email-addr-2))
      (render-new-user-form :error-message "E-mail addresses do not match.")

      (not (= password password-2))
      (render-new-user-form :error-message "Passwords do not match.")

      (core-data/user-email-exists? email-addr)
      (render-new-user-form :error-message "User with this e-mail address already exists.")

      :else
      (do
        (let [user-id (create-user friendly-name email-addr password)]
          (ring/redirect (str "/user/verify/" user-id)))))))

(def date-format (java.text.SimpleDateFormat. "yyyy-MM-dd hh:mm aa"))

(defn render-user-info-form [& {:keys [error-message]}]
  (let [user (core-data/get-user-by-email (auth/current-identity))]
    (render-page {:title "User Information"}
                 (form/form-to
                  [:post "/user/info"]
                  [:input {:type "hidden"
                           :name "username"
                           :value (auth/current-identity)}]
                  [:div.config-panel
                   [:h1 "E-Mail Address"]
                   (auth/current-identity)]

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
                   (.format date-format (or (:last_login_on user) (current-time)))]

                  [:div.config-panel
                   [:a {:href "/user/password"} "Change Password"]]))))

(defn validate-name [name]
  (if name
    (let [name (.trim name)]
      (and (> (.length name) 0)
           (< (.length name) 32)
           name))))

(defn update-user-info [name]
  (if-let [name (validate-name name)]
    (do
      (core-data/set-user-name (auth/current-identity) name)
      (ring/redirect "/user/info"))
    (render-user-info-form :error-message "Invalid Name")))

(defn render-change-password-form  [& {:keys [error-message]}]
  (let [user (core-data/get-user-by-email (auth/current-identity))]
    (render-page {:title "Change Password"}
                 (form/form-to {:class "auth-form"
                                :data-turbo "false"}
                               [:post "/user/password"]
                               [:input {:type "hidden"
                                        :name "username"
                                        :value (auth/current-identity)}]
                               [:div.config-panel
                                [:h1 "E-Mail Address"]
                                (auth/current-identity)]
                               [:div.config-panel
                                [:h1 "Name"]
                                (:friendly_name user)]
                               [:div.config-panel
                                [:h1 "Last Login"]
                                (.format date-format (or (:last_login_on user) (current-time)))]
                               [:div.config-panel
                                [:h1 "Change Password"]
                                (form/password-field {:placeholder "Password"} "password")
                                (form/password-field {:placeholder "New Password"} "new-password")
                                (form/password-field {:placeholder "Verify Password"} "new-password-2")
                                (when error-message
                                  [:div.error-message error-message])
                                [:div
                                 (form/submit-button {} "Change Password")]]))))

(defn- change-password [{:keys [password new-password new-password-2]}]

  (let [username (auth/current-identity)]
    (cond
      (not (auth/get-user-by-credentials {:username username :password password}))
      (render-change-password-form
       :error-message "Old password incorrect.")

      (= password new-password-2)
      (render-change-password-form
       :error-message "New password cannot be the same as the old.")

      (not (= new-password new-password-2))
      (render-change-password-form
       :error-message "Passwords do not match.")

      :else
      (do
        ;; The password change handling is done in a Friend workflow
        ;; handler (password-change-handler), so that it can
        ;; reauthenticate the user against the new password and assign
        ;; the user the correct roles for an account with a valid
        ;; password. (This is needed so that we allow the user use the
        ;; website, if their password had expired.)
        (log/warn "Password change unexpectedly fell through workflow!")
        (ring/redirect "/")))))

(defn render-password-change-success []
  (render-page {:title "Password Successfully Changed"}
               [:div.page-message
                [:h1 "Password Successfully Changed"]
                [:p "Your password has been changed. You can view "
                 "your lists" [:a {:href "/"} " here"] "."]]))

(defn- get-link-verified-user [link-user-id link-uuid]
  (when-let [user-id (:verifies_user_id (core-data/get-verification-link-by-uuid link-uuid))]
    (when (= link-user-id user-id)
      (core-data/get-user-by-id user-id))))

(defn render-password-reset-form [link-user-id link-uuid error-message]
  (when-let [user (get-link-verified-user link-user-id link-uuid)]
    (render-page {:title "Reset Password"}
                 [:div.page-message
                  (form/form-to {:class "auth-form"
                                 :data-turbo "false"}
                                [:post (str "/user/password-reset/" (:user_id user))]
                                [:div.config-panel
                                 [:h1 "Reset your password"]
                                 [:input {:type "hidden"
                                          :name "link_uuid"
                                          :value link-uuid}]
                                 (form/password-field {:placeholder "New Password"} "new-password")
                                 (form/password-field {:placeholder "Verify Password"} "new-password-2")
                                 (when error-message
                                   [:div#error.error-message
                                    error-message])
                                 (form/submit-button {} "Reset Password")])])))

(defn password-reset [user-id link-uuid new-password new-password-2]
  (let [user (get-link-verified-user user-id link-uuid)]
    (cond
      (not user)
      nil

      (not (= new-password new-password-2))
      (render-password-reset-form user-id link-uuid "Passwords do not match.")

      :else
      (do
        (auth/set-user-password (:email_addr user) new-password)
        ;; Resetting the password via a link also serves to
        ;; unlock the account.
        (core-data/reset-login-failures (:user_id user))
        (ring/redirect "/user/password-reset-success")))))

(defn- ensure-verification-link [user-id]
  (unless (core-data/get-verification-link-by-user-id user-id)
          (core-data/create-verification-link user-id)))

(defn- development-verification-form [user-id]
  [:div.dev-tool
   (let [link-uuid (:link_uuid (core-data/get-verification-link-by-user-id user-id))]
     [:a {:href (str "/user/verify/" user-id "/" link-uuid)} "Verify"])])

(defn- development-unlock-form [user-id]
  [:div.dev-tool
   (let [link-uuid (:link_uuid (core-data/get-verification-link-by-user-id user-id))]
     [:a {:href (str "/user/unlock/" user-id "/" link-uuid)} "Unlock"])])

(defn- development-reset-form [user-id]
  [:div.dev-tool
   (let [link-uuid (:link_uuid (core-data/get-verification-link-by-user-id user-id))]
     [:a {:href (str "/user/reset/" user-id "/" link-uuid)} "Reset"])])

(defn- development-no-user-form []
  [:div.dev-tool
   "No user with this e-mail address exists"])

(defn enter-verify-workflow [user-id]
  (let [user (core-data/get-user-by-id user-id)]
    (ensure-verification-link user-id)
    (send-verification-link user-id)
    (render-page {:title "e-Mail Address Verification"}
                 [:div.page-message
                  [:h1 "e-Mail Address Verification"]
                  [:p "An e-mail has been sent to "  [:span.addr (:email_addr user)]
                   " with a link you may use to verify your e-mail address. Please"
                   " check your spam filter if it does not appear within a few minutes."]
                  [:a {:href "/"} "Login"]
                  (when (config/cval :development-mode)
                    (development-verification-form user-id))])))

(defn verify-user [link-user-id link-uuid]
  (when-let [user (get-link-verified-user link-user-id link-uuid)]
    (core-data/add-user-roles (:user_id user) #{:role/verified})
    (render-page {:title "e-Mail Address Verified"}
                 [:div.page-message
                  [:h1 "e-Mail Address Verified"]
                  [:p "Thank you for verifying your e-mail address at: "
                   [:span.addr (:email_addr user)] ". Using the link below, you "
                   "can log in and start to use the system."]
                  [:a {:href "/"} "Login"]])))

(defn enter-unlock-workflow [user-id]
  (let [user (core-data/get-user-by-email (auth/current-identity))]
    (ensure-verification-link user-id)
    (send-unlock-link user-id)
    (render-page {:title "Unlock Account"}
                 [:div.page-message
                  [:h1 "Unlock Account"]
                  [:p "An e-mail has been sent to "  [:span.addr (:email_addr user)]
                   " with a link you may use to unlock your account. Please"
                   " check your spam filter if it does not appear within a few minutes."]
                  [:a {:href "/"} "Login"]
                  (when (config/cval :development-mode)
                    (development-unlock-form user-id))])))

(defn unlock-user [link-user-id link-uuid]
  (when-let [user (get-link-verified-user link-user-id link-uuid)]
    (core-data/reset-login-failures (:user_id user))
    (render-page {:title "Account Unlocked"}
                 [:div.page-message
                  [:h1 "Account Unlocked"]
                  [:p "Thank you for unlocking your account at: "
                   [:span.addr (:email_addr user)] ". Using the link below, you "
                   "can log in and start to use the system."]
                  [:a {:href "/"} "Login"]])))

(defn enter-password-reset-workflow [email-addr]
  (let [user (core-data/get-user-by-email email-addr)
        user-id (and user (:user_id user))]
    (when user-id
      (ensure-verification-link user-id)
      (send-reset-link user-id))
    (render-page {:title "Reset Password"}
                 [:div.page-message
                  [:h1 "Reset Password"]
                  [:p "If there is an account with this e-mail address, an e-mail"
                   " has been sent with a link you may use to reset your password. Please"
                   " check your spam filter if it does not appear within a few minutes."]
                  [:a {:href "/"} "Login"]
                  (when (config/cval :development-mode)
                    (if user-id
                      (development-reset-form user-id)
                      (development-no-user-form)))])))

(defn render-password-reset-success []
  (render-page {:title "Password Successfully Reset"}
               [:div.page-message
                [:h1 "Password Successfully Reset"]
                [:p "Your password has been reset. You can login "
                 [:a {:href "/"} "here"] "."]]))

(defn- support-message [params]
  [:body
   [:h1
    "Todo List - Support Request"]
   [:p
    (:full-name params) ","]
   [:p]
   [:p
    "Thank you for contacting support, we will be in "
    "touch soon. The contents of your message are below."]
   [:p
    "-- Todo Support"]
   [:hr]
   [:h2 "Message:"]
   [:p (:message-text params)]
   [:p
    "Site URL:" [:tt (:current-uri params)]]
   [:p
    "E-Mail:" [:tt (:email-address params)]]])

(defn- send-support-message [params]
  (let [message  {:subject "Support Request"
                  :content support-message
                  :params params}]
    (log/info "Sending support message:" (:email-addr params)
              "Regarding URI:" (:current-uri params))
    (mail/send-email (assoc message :to (config/cval :admin-mails)))
    (mail/send-email (assoc message :to (:email-address params)))))

(defn- handle-support-message [params]
  (if (verify-response-correct params)
    (send-support-message params)
    (log/warn "Non verified request" params))
  (ring/redirect (or (uri-path? (:current-uri params))
                     "/")))

(defn- notice-gdpr-consent [req]
  (assoc (ring/redirect (or (get-in req [:headers "referer"])
                            "/"))
         :session {:gdpr-consent "yes"}))

(defn- render-gdpr-consent-decline []
  (render-page {:title "Cookie Consent Declined"
                :suppress-gdpr-consent true}
               [:div.page-message
                [:h1 "Cookie Consent Declined"]
                [:p
                 "You have declined consent for this website's use of cookies. "
                 "However, cookies are required for this site to function "
                 "properly and securely. Please accept our apologies for any "
                 "inconvenience this may cause. If you change your mind, you can "
                 "do so " [:a {:href "/"} "here"] ". Otherwise, this will take you "
                 " to " [:a {:href "https://www.google.com"} "Google"] "."]]))

(defroutes private-routes
  (GET "/user/password" []
    (render-change-password-form))

  (GET "/user/password-changed" []
    (render-password-change-success))

  (POST "/user/password" {params :params}
    (change-password params))

  (GET "/user/info" []
    (render-user-info-form))

  (POST "/user/info" {{name :name} :params}
    (update-user-info name))

  (POST "/contact-support" {params :params}
    (handle-support-message params)))

(defroutes all-routes
  (GET "/user" []
    (render-new-user-form))

  (POST "/user" {params :params}
    (add-user params))

  (GET "/login" {{login-failed :login_failed email-addr :username} :params}
    (render-login-page :email-addr email-addr
                       :login-failure? (= login-failed "Y")))

  (POST "/user/gdpr-consent" req
    (notice-gdpr-consent req))

  (GET "/user/gdpr-consent-decline" req
    (render-gdpr-consent-decline))

  ;; User Verification Workflow
  (GET "/user/verify/:user-id" {{user-id :user-id} :params}
    (enter-verify-workflow user-id))

  (friend/logout
   (GET "/user/verify/:user-id/:link-uuid" {{user-id :user-id link-uuid :link-uuid} :params}
     (verify-user (try-parse-integer user-id) link-uuid)))

  ;; Account Unlock workflow
  (GET "/user/unlock/:user-id" {{user-id :user-id} :params}
    (enter-unlock-workflow user-id))

  (friend/logout
   (GET "/user/unlock/:user-id/:link-uuid" {{user-id :user-id link-uuid :link-uuid} :params}
     (unlock-user (try-parse-integer user-id) link-uuid)))

  ;; Password Reset Workflow
  (GET "/user/forgot-password" []
    (render-forgot-password-form))

  (POST "/user/password-reset" {{email-addr :email-addr} :params}
    (enter-password-reset-workflow email-addr))

  (POST "/user/password-reset/:user-id" {params :params}
    (password-reset (try-parse-integer (:user-id params)) (:link_uuid params)
                    (:new-password params) (:new-password-2 params)))

  (friend/logout
   (GET "/user/reset/:user-id/:link-uuid" {{user-id :user-id link-uuid :link-uuid error-message :error-message} :params}
     (render-password-reset-form (try-parse-integer user-id) link-uuid error-message)))

  (GET "/user/password-reset-success" []
    (render-password-reset-success))

  ;; Logout Link
  (friend/logout
   (ANY "/logout" [] (ring/redirect "/")))

  ;; Secure Links
  (wrap-routes private-routes friend/wrap-authorize #{:role/verified}))
