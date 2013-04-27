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

(defn all-user-names []
  (sql/with-connection hsql-db
    (sql/with-query-results rows
      ["select name from user order by name"]
      (doall (map :name rows)))))

(defn table-info [table-name]
  (sql/with-connection hsql-db
    (sql/with-query-results rows
      ["select * from information_schema.tables where table_name=?" table-name]
      (doall rows))))

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

(defn add-user [name password email-addr]
  (sql/with-connection hsql-db
    (:user_id (first
               (sql/insert-records
                :user
                {:name name
                 :password password
                 :email_addr email-addr})))))

(defn get-user-by-name [ user-name ]
  (sql/with-connection hsql-db
    (sql/with-query-results rows
      ["select * from user where name=?" user-name]
      (first rows))))

(defn add-todo-item [ user-id desc ]
  (sql/with-connection hsql-db
    (:item_id (first
               (sql/insert-records
                :todo_item
                {:user_id user-id
                 :desc desc
                 :completed false})))))

(defn get-pending-items [ ]
  (sql/with-connection hsql-db
    (sql/with-query-results rows
      ["select user_id, desc from todo_item where completed=false" ]
      (doall rows))))

(defn get-item-by-id [ item-id ]
  (sql/with-connection hsql-db
    (sql/with-query-results rows
      ["select * from todo_item where item_id=?" item-id ]
      (doall rows))))

(defn complete-item-by-id [ item-id ]
  (sql/with-connection hsql-db
    (sql/update-values
     :todo_item
     [ "item_id=?" item-id]
     { :completed true })))

;; (defn remove-item-by-id [ item-id ]
;;   (sql/with-connection hsql-db
;;     (sql/delete
;;      :todo_item
;;      [ "item_id=?" item-id])))

