(ns toto.data
  (:use toto.util
        sql-file.sql-util)
  (:require [clojure.tools.logging :as log]
            [clojure.java.jdbc :as jdbc]
            [sql-file.core :as sql-file]
            [toto.queries :as query]))

(def db-connection
  (delay (-> (sql-file/open-pool {:name (config-property "db.subname" "toto")
                                  :schema-path [ "sql/" ]})
             (sql-file/ensure-schema [ "toto" 0 ]))))

(def ^:dynamic *db* nil)

(defn call-with-db-connection [ fn ]
  (jdbc/with-db-connection [ conn @db-connection ]
    (binding [ *db* conn]
      (fn))))

(defmacro with-db-connection [ & body ]
  `(call-with-db-connection (fn [] ~@body)))

(defn wrap-db-connection [ app ]
  (fn [ req ]
    (with-db-connection
      (app req))))

(defn- scalar
  ([ query-result default ]
   (or
    (let [first-row (first query-result)]
      (get first-row (first (keys first-row))))
    default))

  ([ query-result ]
   (scalar query-result nil)))

(defn current-time []
  (java.util.Date.))

;;; user

(defn get-user-roles [ user-id ]
  (set
   (map #(keyword "toto.role" (:role_name %))
        (query-all *db*
                   [(str "SELECT role_name"
                         "  FROM user u, role r, user_role ur"
                         "  WHERE u.user_id = ur.user_id"
                         "    AND ur.role_id = r.role_id"
                         "    AND u.user_id = ?")
                    user-id]))))

(defn- get-role-id [ role-name ]
  (query-scalar *db*
                [(str "SELECT role_id"
                      "  FROM role"
                      " WHERE role_name = ?")
                 (name role-name)]))

(defn delete-user-roles [ user-id ]
  (jdbc/delete! *db* :user_role ["user_id=?" user-id]))


(defn set-user-roles [ user-id role-set ]
  (jdbc/with-db-transaction [ trans *db* ]
    (delete-user-roles user-id)
    (doseq [ role-id (map get-role-id role-set)]
      (jdbc/insert! *db* :user_role
                    {:user_id user-id
                     :role_id role-id}))))


(defn add-user-roles [ user-id role-set ]
  (set-user-roles user-id (clojure.set/union (get-user-roles user-id)
                                             role-set)))


(defn get-user-by-email [ email-addr ]
  (first
   (query/get-user-by-email { :email_addr email-addr}
                            { :connection *db* })))

(defn user-email-exists? [ email-addr ]
  (not (nil? (get-user-by-email email-addr))))

(defn get-user-by-id [ user-id ]
  (first
   (query/get-user-by-id { :user_id user-id }
                         { :connection *db* })))

(defn add-user [ email-addr password ]
  (:user_id (first
             (jdbc/insert! *db*
              :user
              {:email_addr email-addr
               :password password
               :account_created_on (current-time)
               :password_created_on (current-time)
               :friendly_name email-addr}))))

(defn set-user-password [ email-addr password ]
  (jdbc/update! *db* :user
                {:password password
                 :password_created_on (current-time)}
                ["email_addr=?" email-addr]))

(defn set-user-name [ email-addr name ]
  (jdbc/update! *db* :user
                {:friendly_name name}
                ["email_addr=?" email-addr]))

(defn set-user-login-time [ email-addr ]
  (jdbc/update! *db* :user
                {:last_login_on (current-time)}
                ["email_addr=?" email-addr]))

(defn create-verification-link [ user-id ]
  (:verification_link_id
   (first
    (jdbc/insert! *db* :verification_link
                  {:link_uuid (.toString (java.util.UUID/randomUUID))
                   :verifies_user_id user-id
                   :created_on (current-time)}))))

(defn get-verification-link-by-user-id [ user-id ]
  (query-first *db* [(str "SELECT *"
                          "  FROM verification_link"
                          " WHERE verifies_user_id=?")
                     user-id]))

(defn get-verification-link-by-id [ link-id ]
  (query-first *db* [(str "SELECT *"
                          "  FROM verification_link"
                          " WHERE verification_link_id=?")
                     link-id]))

(defn get-verification-link-by-uuid [ link-uuid ]
  (query-first *db* [(str "SELECT *"
                          "  FROM verification_link"
                          " WHERE link_uuid=?")
                     link-uuid]))
