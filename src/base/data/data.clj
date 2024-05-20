;; Copyright (c) 2015-2023 Michael Schaeffer (dba East Coast Toolworks)
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

(ns base.data.data
  (:use playbook.core
        sql-file.sql-util
        sql-file.middleware)
  (:require [taoensso.timbre :as log]
            [clojure.java.jdbc :as jdbc]
            [sql-file.core :as sql-file]
            [base.data.queries :as query]))

(defn get-user-roles [ user-id ]
  (set
   (map #(keyword "toto.role" (:role_name %))
        (query-all (current-db-connection)
                   [(str "SELECT role_name"
                         "  FROM user u, role r, user_role ur"
                         "  WHERE u.user_id = ur.user_id"
                         "    AND ur.role_id = r.role_id"
                         "    AND u.user_id = ?")
                    user-id]))))

(defn- get-role-id [ role-name ]
  (query-scalar (current-db-connection)
                [(str "SELECT role_id"
                      "  FROM role"
                      " WHERE role_name = ?")
                 (name role-name)]))

(defn delete-user-roles [ user-id ]
  (jdbc/delete! (current-db-connection) :user_role ["user_id=?" user-id]))


(defn set-user-roles [ user-id role-set ]
  (jdbc/with-db-transaction [ trans (current-db-connection) ]
    (delete-user-roles user-id)
    (doseq [ role-id (map get-role-id role-set)]
      (jdbc/insert! (current-db-connection) :user_role
                    {:user_id user-id
                     :role_id role-id}))))

(defn add-user-roles [ user-id role-set ]
  (set-user-roles user-id (clojure.set/union (get-user-roles user-id)
                                             role-set)))

(defn get-user-by-email [ email-addr ]
  (first
   (query/get-user-by-email { :email_addr email-addr}
                            { :connection (current-db-connection) })))

(defn user-email-exists? [ email-addr ]
  (not (nil? (get-user-by-email email-addr))))

(defn get-user-by-id [ user-id ]
  (first
   (query/get-user-by-id { :user_id user-id }
                         { :connection (current-db-connection) })))

(defn add-user [ friendly-name email-addr password ]
  (:user_id (first
             (jdbc/insert! (current-db-connection)
              :user
              {:email_addr email-addr
               :password password
               :account_created_on (current-time)
               :password_created_on (current-time)
               :friendly_name friendly-name}))))

(defn set-user-password [ email-addr password ]
  (jdbc/update! (current-db-connection) :user
                {:password password
                 :password_created_on (current-time)}
                ["email_addr=?" email-addr]))

(defn set-user-name [ email-addr name ]
  (jdbc/update! (current-db-connection) :user
                {:friendly_name name}
                ["email_addr=?" email-addr]))

(defn record-user-login [ email-addr login-ip ]
  (jdbc/update! (current-db-connection) :user
                {:last_login_on (current-time)
                 :last_login_ip login-ip}
                ["email_addr=?" email-addr]))

(defn record-user-login-failure [ user-id request-ip ]
  (jdbc/insert! (current-db-connection) :login_failure
                {:user_id user-id
                 :failed_on (current-time)
                 :request_ip request-ip}))

(defn reset-login-failures [ user-id ]
  (jdbc/delete! (current-db-connection)
                :login_failure
                ["user_id=?" user-id]))

(defn create-verification-link [ user-id ]
  (:verification_link_id
   (first
    (jdbc/insert! (current-db-connection) :verification_link
                  {:link_uuid (.toString (java.util.UUID/randomUUID))
                   :verifies_user_id user-id
                   :created_on (current-time)}))))

(defn get-verification-link-by-user-id [ user-id ]
  (query-first (current-db-connection) [(str "SELECT *"
                          "  FROM verification_link"
                          " WHERE verifies_user_id=?")
                     user-id]))

(defn get-verification-link-by-id [ link-id ]
  (query-first (current-db-connection) [(str "SELECT *"
                          "  FROM verification_link"
                          " WHERE verification_link_id=?")
                     link-id]))

(defn get-verification-link-by-uuid [ link-uuid ]
  (query-first (current-db-connection) [(str "SELECT *"
                          "  FROM verification_link"
                          " WHERE link_uuid=?")
                     link-uuid]))

(defn delete-old-verification-links []
  (jdbc/delete! (current-db-connection)
                :verification_link
                ["created_on < DATEADD('hour', -1, CURRENT_TIMESTAMP)"]))
