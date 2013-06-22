(ns toto.data
  (:require [clojure.java.jdbc :as jdbc]
            [toto.schema :as schema]))

(defn all-user-names []
  (jdbc/with-connection schema/hsql-db
    (jdbc/with-query-results rows
      ["select name from user order by name"]
      (doall (map :name rows)))))

(defn all-users []
  (jdbc/with-connection schema/hsql-db
    (jdbc/with-query-results rows
      ["select * from user order by email_addr"]
      (doall rows))))

(defn table-info [ table-name ]
  (jdbc/with-connection schema/hsql-db
    (jdbc/with-query-results rows
      ["select * from information_schema.tables where table_name=?" table-name]
      (doall rows))))

(defn get-user-by-email [ email-addr ]
  (jdbc/with-connection schema/hsql-db
    (jdbc/with-query-results rows
      ["select * from user where email_addr=?" email-addr]
      (first rows))))

(defn get-user-by-id [ id ]
  (jdbc/with-connection schema/hsql-db
    (jdbc/with-query-results rows
      ["select * from user where user_id=?" id]
      (first rows))))

(defn get-todo-list-ids-by-user [ user-id ]
  (jdbc/with-connection schema/hsql-db
    (jdbc/with-query-results rows
      ["select todo_list_id from todo_list_owners where user_id=?" user-id]
      (map :todo_list_id rows))))

(defn add-user [ email-addr password ]
  (jdbc/with-connection schema/hsql-db
    (:user_id (first
               (jdbc/insert-records
                :user
                {:email_addr email-addr
                 :password password})))))

(defn add-list [ desc ]
  (jdbc/with-connection schema/hsql-db
    (:todo_list_id (first
                    (jdbc/insert-records
                     :todo_list
                     {:desc desc})))))

(defn add-list-owner [ user-id todo-list-id ]
  (jdbc/with-connection schema/hsql-db
    (:todo_list_id (first
                    (jdbc/insert-records
                     :todo_list_owners
                     {:user_id user-id
                      :todo_list_id todo-list-id})))))

(defn create-user  [ email-addr password ]
  (let [uid (add-user email-addr password)
        list-id (add-list "User Todo List")]
    (add-list-owner uid list-id)
    uid))

(defn add-todo-item [ todo-list-id desc ]
  (jdbc/with-connection schema/hsql-db
    (:item_id (first
               (jdbc/insert-records
                :todo_item
                {:todo_list_id todo-list-id
                 :desc desc
                 :completed false})))))

(defn get-pending-items [ list-id ]
  (jdbc/with-connection schema/hsql-db
    (jdbc/with-query-results rows
      ["select item_id, todo_list_id, desc from todo_item where completed=false and todo_list_id=?" list-id ]
      (doall rows))))

(defn complete-item-by-id [ item-id ]
  (jdbc/with-connection schema/hsql-db
    (jdbc/update-values
     :todo_item
     ["item_id=?" item-id]
     {:completed true})))

(defn update-item-by-id [ item-id item-description ]
  (jdbc/with-connection schema/hsql-db
    (jdbc/update-values
     :todo_item
     ["item_id=?" item-id]
     {:desc item-description})))

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