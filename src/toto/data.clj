(ns toto.data
  (:require [clojure.java.jdbc :as sql]
            [toto.schema :as schema]))


(defn all-user-names []
  (sql/with-connection schema/hsql-db
    (sql/with-query-results rows
      ["select name from user order by name"]
      (doall (map :name rows)))))

(defn table-info [table-name]
  (sql/with-connection schema/hsql-db
    (sql/with-query-results rows
      ["select * from information_schema.tables where table_name=?" table-name]
      (doall rows))))

(defn add-user [name password email-addr]
  (sql/with-connection schema/hsql-db
    (:user_id (first
               (sql/insert-records
                :user
                {:name name
                 :password password
                 :email_addr email-addr})))))

(defn get-user-by-name [ user-name ]
  (sql/with-connection schema/hsql-db
    (sql/with-query-results rows
      ["select * from user where name=?" user-name]
      (first rows))))

(defn add-todo-item [ user-id desc ]
  (sql/with-connection schema/hsql-db
    (:item_id (first
               (sql/insert-records
                :todo_item
                {:user_id user-id
                 :desc desc
                 :completed false})))))

(defn get-pending-items [ ]
  (sql/with-connection schema/hsql-db
    (sql/with-query-results rows
      ["select user_id, desc from todo_item where completed=false" ]
      (doall rows))))

(defn get-item-by-id [ item-id ]
  (sql/with-connection schema/hsql-db
    (sql/with-query-results rows
      ["select * from todo_item where item_id=?" item-id ]
      (doall rows))))

(defn complete-item-by-id [ item-id ]
  (sql/with-connection schema/hsql-db
    (sql/update-values
     :todo_item
     [ "item_id=?" item-id]
     { :completed true })))

;; TODO: remove-item-by-id