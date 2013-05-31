(ns toto.data
  (:require [clojure.java.jdbc :as jdbc]
            [toto.schema :as schema]))

(defn all-user-names []
  (jdbc/with-connection schema/hsql-db
    (jdbc/with-query-results rows
      ["select name from user order by name"]
      (doall (map :name rows)))))

(defn table-info [table-name]
  (jdbc/with-connection schema/hsql-db
    (jdbc/with-query-results rows
      ["select * from information_schema.tables where table_name=?" table-name]
      (doall rows))))

(defn add-user [name password email-addr]
  (jdbc/with-connection schema/hsql-db
    (:user_id (first
               (jdbc/insert-records
                :user
                {:name name
                 :password password
                 :email_addr email-addr})))))

(defn get-user-by-name [ user-name ]
  (jdbc/with-connection schema/hsql-db
    (jdbc/with-query-results rows
      ["select * from user where name=?" user-name]
      (first rows))))

(defn add-user [username email-addr password]
  (jdbc/with-connection schema/hsql-db
    (:item_id (first
               (jdbc/insert-records
                :user
                {:name username
                 :password password
                 :email_addr email-addr})))))


(defn add-todo-item [ user-id desc ]
  (jdbc/with-connection schema/hsql-db
    (:item_id (first
               (jdbc/insert-records
                :todo_item
                {:user_id user-id
                 :desc desc
                 :completed false})))))

(defn get-pending-items [ ]
  (jdbc/with-connection schema/hsql-db
    (jdbc/with-query-results rows
      ["select item_id, user_id, desc from todo_item where completed=false" ]
      (doall rows))))

(defn get-item-by-id [ item-id ]
  (jdbc/with-connection schema/hsql-db
    (jdbc/with-query-results rows
      ["select * from todo_item where item_id=?" item-id ]
      (first (doall rows)))))

(defn complete-item-by-id [ item-id ]
  (jdbc/with-connection schema/hsql-db
    (jdbc/update-values
     :todo_item
     [ "item_id=?" item-id]
     { :completed true })))

;; TODO: remove-item-by-id