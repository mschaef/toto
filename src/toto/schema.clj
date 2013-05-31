(ns toto.schema
  (:require [clojure.java.jdbc :as jdbc]))

(def hsql-db {:subprotocol "hsqldb"
              :subname "~/ectworks/toto/toto.h2db"
              :user "sa"
              :password ""
              })

(defn setup-schema []
 (jdbc/with-connection hsql-db

   (jdbc/create-table
    :toto_schema_version
    [:version_number "BIGINT"])

   (jdbc/insert-records
    :toto_schema_version
    {:version_number 1})
   
   (jdbc/create-table
    :user
    [:user_id "BIGINT" "identity"]
    [:email_addr "varchar(255)"]
    [:password "varchar(255)"])

   (jdbc/create-table
    :todo_item
    [:item_id "BIGINT" "identity"]
    [:user_id "BIGINT" "references user(user_id)"]
    [:desc "varchar(255)"]
    [:completed "boolean"])
   
   (jdbc/insert-records
    :user
    {:email_addr "schaeffer.michael.a@gmail.com"
     :password "$2a$10$2/IVfmNoQ86gbnVzoJ2oWO8Q1.klpSE6E8Z24uD7YPnFQt9uoirru"})))

(defn version-table-present? []
  (> (jdbc/with-connection hsql-db
       (jdbc/with-query-results rows
         ["select count(table_name) from information_schema.tables where table_name='TOTO_SCHEMA_VERSION'"]
         (first (map :c1 rows))))
     0))

(defn installed-schema-version []
  (jdbc/with-connection hsql-db
    (jdbc/with-query-results rows
      ["select version_number from toto_schema_version"]
      (first (map :version_number rows)))))

(defn ensure-schema-available []
  (if (not (version-table-present?))
    (setup-schema)))

(defn all-table-names []
  (jdbc/with-connection hsql-db
    (jdbc/with-query-results rows
      ["select table_name from information_schema.tables order by table_name"]
      (doall (map :table_name rows)))))
