(ns toto.view.auth
  (:use toto.core.util
        compojure.core)
  (:require [clojure.tools.logging :as log]
            [cemerick.friend.credentials :as credentials]
            [cemerick.friend :as friend]
            [cemerick.friend.workflows :as workflows]
            [toto.data.data :as data]))

(defn get-user-id-by-email [ email ]
  (if-let [ user-info (data/get-user-by-email email) ]
    (user-info :user_id)
    nil))

(defmacro authorize-expected-roles [ & body ]
  `(friend/authorize #{:toto.role/user} ~@body))

(defn authorize-toto-valid-user [ routes ]
  (-> routes
      (wrap-routes friend/wrap-authorize #{:toto.role/user})))

(defn- password-current? [ user-record ]
  (if-let [expiry (:password_expires_on user-record)]
    (or (.after expiry (java.util.Date.))
        (.after (:password_created_on user-record) expiry))
    true))

(defn- account-locked? [ user-record ]
  (> (:login_failure_count user-record) 4))

(defn- get-user-roles [ user-record ]
  (cond (account-locked? user-record)
        #{:toto.role/locked-account}

        (not (password-current? user-record))
        #{:toto.role/password-expired}

        :else
        (clojure.set/union
         #{:toto.role/user} (data/get-user-roles (:user_id user-record)))))

(defn get-auth-map-by-email [ email ]
  (if-let [user-record (data/get-user-by-email email)]
    {:identity email
     :user-record user-record
     :user-id (user-record :user_id)
     :roles (get-user-roles user-record)
     :account-locked (account-locked? user-record)
     :login-failure-count (user-record :login_failure_count)}))

(defn get-user-by-credentials [ creds ]
  (if-let [auth-map (get-auth-map-by-email (creds :username))]
    (cond
      (:account-locked auth-map)
      (update-in auth-map [:user-record] dissoc :password)

      (credentials/bcrypt-verify (creds :password) (get-in auth-map [:user-record :password]))
      (do
        (data/set-user-login-time (creds :username))
        (update-in auth-map [:user-record] dissoc :password))

      :else
      (do
        (data/record-login-failure (:user-id auth-map))
        nil))
    nil))

(defn current-roles []
  (:roles (friend/current-authentication)))

(defn set-user-password [ username password ]
  (data/set-user-password username (credentials/hash-bcrypt password)))

(defn create-user [ email-addr password ]
  (data/add-user email-addr (credentials/hash-bcrypt password)))

(defn password-change-workflow []
  (fn [{:keys [uri request-method params]}]
    (when (and (= uri "/user/password-change")
               (= request-method :post)
               (get-user-by-credentials params)
               (not (= (:password params) (:new_password1 params)))
               (= (:new_password1 params) (:new_password2 params)))
      (set-user-password (:username params) (:new_password1 params))
      (workflows/make-auth (get-auth-map-by-email (:username params))))))

(defn wrap-authenticate [ request unauthorized-handler ]
  (friend/authenticate request
                       {:credential-fn get-user-by-credentials
                        :workflows [(password-change-workflow)
                                    (workflows/interactive-form)]
                        :unauthorized-handler unauthorized-handler}))
