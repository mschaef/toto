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

(ns toto.data.data
  (:use playbook.core
        sql-file.sql-util
        sql-file.middleware)
  (:require [taoensso.timbre :as log]
            [sql-file.core :as sql-file]
            [toto.data.queries :as query]))

(defn get-user-roles [user-id]
  (set
   (map #(keyword "role" (:role_name %))
        (query/get-user-roles {:user_id user-id}))))

(defn- get-role-id [role-name]
  (scalar-result
   (query/get-role-id {:role_name (name role-name)})))

(defn- delete-user-roles! [user-id]
  (query/delete-user-role! {:user_id user-id}))

(defn- insert-user-role! [user-id role-id]
  (query/insert-user-role! {:user_id user-id
                            :role_id role-id}))

(defn set-user-roles [user-id role-set]
  (with-db-transaction
    (delete-user-roles! user-id)
    (doseq [role-id (map get-role-id role-set)]
      (insert-user-role! user-id role-id))))

(defn add-user-roles [user-id role-set]
  (set-user-roles user-id (clojure.set/union (get-user-roles user-id)
                                             role-set)))

(defn get-user-by-email [email-addr]
  (first
   (query/get-user-by-email {:email_addr email-addr})))

(defn user-email-exists? [email-addr]
  (not (nil? (get-user-by-email email-addr))))

(defn get-user-by-id [user-id]
  (first
   (query/get-user-by-id {:user_id user-id})))

(defn add-user [friendly-name email-addr password]
  (let [now (current-time)]
    (:user_id (query/add-user<! {:email_addr email-addr
                                 :password password
                                 :account_created_on now
                                 :password_created_on now
                                 :friendly_name friendly-name}))))

(defn set-user-password [email-addr password]
  (query/set-user-password! {:password password
                             :password_created_on (current-time)
                             :email_addr email-addr}))

(defn set-user-name [email-addr name]
  (query/set-user-name! {:email_addr email-addr
                         :friendly_name name}))

(defn record-user-login [email-addr login-ip]
  (query/record-user-login! {:last_login_on (current-time)
                             :last_login_ip login-ip
                             :email_addr email-addr}))

(defn record-user-login-failure [user-id request-ip]
  (query/record-user-login-failure! {:user_id user-id
                                     :failed_on (current-time)
                                     :request_ip request-ip}))

(defn reset-login-failures [user-id]
  (query/reset-login-failures! {:user_id user-id}))

(defn create-verification-link [user-id]
  (:verification_link_id
   (query/create-verification-link<! {:link_uuid (.toString (java.util.UUID/randomUUID))
                                      :verifies_user_id user-id
                                      :created_on (current-time)})))

(defn get-verification-link-by-user-id [user-id]
  (first
   (query/get-verification-link-by-user-id {:user_id user-id})))

(defn get-verification-link-by-id [link-id]
  (first
   (query/get-verification-link-by-id {:link_id link-id})))

(defn get-verification-link-by-uuid [link-uuid]
  (first
   (query/get-verification-link-by-uuid {:link_uuid link-uuid})))

(defn delete-old-verification-links []
  (query/delete-old-verification-links!))

(defn get-unverified-users-by-id [max-unverified-age]
  (map :user_id
       (query/get-unverified-users-by-id {:max_unverified_age max-unverified-age})))

(defn delete-user-by-id! [user-id]
  (query/delete-user-by-id! {:user_id user-id}))
