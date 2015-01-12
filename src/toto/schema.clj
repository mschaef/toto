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

(defmacro with-db-connection [ binding & body ]
  `(jdbc/with-db-connection [~binding db-connection ]
     ~@body))

(defn setup-schema []
  (log/warn "Creating new schema instance")
  (with-db-connection db
    (jdbc/db-do-commands db
                         (jdbc/create-table-ddl
                          :toto_schema_version
                          [:version_number "BIGINT"]))

   (jdbc/insert! db
    :toto_schema_version
    {:version_number 1})

   (jdbc/db-do-commands db
                        (jdbc/create-table-ddl
                         :user
                         [:user_id "BIGINT" "IDENTITY"]
                         [:email_addr "VARCHAR(255)" "UNIQUE"]
                         [:password "VARCHAR(255)"]))

   (jdbc/db-do-commands db
                        (jdbc/create-table-ddl
                         :todo_list
                         [:todo_list_id "BIGINT" "IDENTITY"]
                         [:desc "VARCHAR(32)"]))

   (jdbc/db-do-commands db
                        (jdbc/create-table-ddl
                         :todo_view
                         [:view_id "BIGINT" "IDENTITY"]
                         [:user_id "BIGINT" "REFERENCES user(user_id)"]
                         [:view_name "VARCHAR(32)" "NOT NULL"]))

   (jdbc/db-do-commands db 
                        (jdbc/create-table-ddl
                         :todo_view_lists
                         [:view_id "BIGINT" "NOT NULL" "REFERENCES todo_view(view_id)"]
                         [:todo_list_id "BIGINT" "NOT NULL" "REFERENCES todo_list(todo_list_id)"]
                         [:list_order "INT" "NOT NULL"]
                         ["PRIMARY KEY(view_id, todo_list_id)"]))

   (jdbc/db-do-commands db
                        (jdbc/create-table-ddl
                         :todo_list_owners
                         [:todo_list_id "BIGINT" "NOT NULL" "REFERENCES todo_list(todo_list_id)"]
                         [:user_id "BIGINT" "NOT NULL" "REFERENCES user(user_id)"]
                         ["PRIMARY KEY(todo_list_id, user_id)"]))

   (jdbc/db-do-commands db
                        (jdbc/create-table-ddl
                         :todo_item
                         [:item_id "BIGINT" "IDENTITY"]
                         [:todo_list_id "BIGINT" "NOT NULL" "REFERENCES todo_list(todo_list_id)"]
                         [:desc "VARCHAR(1024)" "NOT NULL"]
                         [:priority "TINYINT" "NOT NULL"]
                         [:created_on "TIMESTAMP" "NOT NULL"]))

   (jdbc/db-do-commands db
                        (jdbc/create-table-ddl
                         :todo_item_completion
                         [:item_id "BIGINT" "UNIQUE" "REFERENCES todo_item(item_id)"]
                         [:user_id "BIGINT" "REFERENCES user(user_id)"]
                         [:completed_on "TIMESTAMP" "NOT NULL"]
                         [:is_delete "BOOLEAN" "NOT NULL"]))))

(defn version-table-present? []
  (> (with-db-connection db
       (query-scalar db
                     ["select count(table_name) from information_schema.tables where table_name='TOTO_SCHEMA_VERSION'"]))
     0))

(defn installed-schema-version []
  (with-db-connection db
    (:version_number
     (query-first db ["select version_number from toto_schema_version"]))))

(defn ensure-schema-available []
  (log/trace "Ensuring Schema Available @ " (log-safe-db-connection))
  (unless (version-table-present?)
     (setup-schema)))

