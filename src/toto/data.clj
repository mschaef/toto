(ns toto.data
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as sql]))

(def hsql-db {:subprotocol "hsqldb"
              :subname "~/ectworks/toto/toto.h2db"
              :user "sa"
              :password ""
              })

(defn all-table-names []
  (sql/with-connection hsql-db
    (sql/with-query-results rows
      ["select table_name from information_schema.tables order by table_name"]
      (doall (map :table_name rows)))))


(defn setup-schema []
 (sql/with-connection hsql-db
   (sql/create-table
    :user
    [:user_id "BIGINT" "identity"]
    [:name "varchar(255)" "unique"]
    [:password "varchar(255)"]
    [:email_addr "varchar(255)"])

   (sql/create-table
    :todo_item
    [:item_id "BIGINT" "identity"]
    [:user_id "BIGINT" "references user(user_id)"]
    [:desc "varchar(255)"]
    [:completed "boolean"])
   
   (sql/insert-records
    :user
    {:name "mschaef"
     :password "14d5b8f25f499c041a12508a9be7b87e52db818e3a06bf6fe970a7fe7d39a1e5"
     :email_addr "schaeffer.michael.a@gmail.com"})))