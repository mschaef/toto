(ns toto.schema
  (:use toto.util)
  (:require [clojure.tools.logging :as log]
            [clojure.java.jdbc :as jdbc]
            [toto.core :as core]))

(def db-connection {:subprotocol (config-property "db.subprotocol" "hsqldb")
                    :subname (config-property "db.subname" "toto.h2db")
                    :user (config-property "db.user" "sa")
                    :password (config-property "db.password" "")})

(defn log-safe-db-connection []
 (dissoc db-connection :password))

(defmacro with-db-connection [ & body ]
  `(jdbc/with-connection db-connection
     ~@body))

(defn setup-schema []
  (log/warn "Creating new schema instance")
  (with-db-connection
   (jdbc/create-table
    :toto_schema_version
    [:version_number "BIGINT"])

   (jdbc/insert-records
    :toto_schema_version
    {:version_number 1})
   
   (jdbc/create-table
    :user
    [:user_id "BIGINT" "IDENTITY"]
    [:email_addr "VARCHAR(255)" "UNIQUE"]
    [:password "VARCHAR(255)"])

   (jdbc/create-table
    :todo_list
    [:todo_list_id "BIGINT" "IDENTITY"]
    [:desc "VARCHAR(32)"])

   (jdbc/create-table
    :todo_list_owners
    [:todo_list_id "BIGINT" "NOT NULL" "REFERENCES todo_list(todo_list_id)"]
    [:user_id "BIGINT" "NOT NULL" "REFERENCES user(user_id)"]
    ["PRIMARY KEY(todo_list_id, user_id)"])

   (jdbc/create-table
    :todo_item
    [:item_id "BIGINT" "IDENTITY"]
    [:todo_list_id "BIGINT" "NOT NULL" "REFERENCES todo_list(todo_list_id)"]
    [:desc "VARCHAR(1024)" "NOT NULL"]
    [:created_on "TIMESTAMP" "NOT NULL"])

   (jdbc/create-table
    :todo_item_completion
    [:item_id "BIGINT" "UNIQUE" "REFERENCES todo_item(item_id)"]
    [:user_id "BIGINT" "REFERENCES user(user_id)"]
    [:completed_on "TIMESTAMP" "NOT NULL"]
    [:is_delete "BOOLEAN" "NOT NULL"])))

(defn version-table-present? []
  (> (with-db-connection
       (jdbc/with-query-results rows
         ["select count(table_name) from information_schema.tables where table_name='TOTO_SCHEMA_VERSION'"]
         (first (map :c1 rows))))
     0))

(defn installed-schema-version []
  (with-db-connection
    (jdbc/with-query-results rows
      ["select version_number from toto_schema_version"]
      (first (map :version_number rows)))))

(defn ensure-schema-available []
  (log/trace "Ensuring Schema Available @ " (log-safe-db-connection))
  (unless (version-table-present?)
     (setup-schema)))

(defn all-table-names []
  (with-db-connection
    (jdbc/with-query-results rows
      ["select table_name from information_schema.tables order by table_name"]
      (doall (map :table_name rows)))))

