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

(defn user-email-exists? [ email-addr ]
  (not (nil? (get-user-by-email email-addr))))

(defn get-user-by-id [ user-id ]
  (jdbc/with-connection schema/hsql-db
    (jdbc/with-query-results rows
      ["select * from user where user_id=?" user-id]
      (first rows))))

(defn get-todo-list-by-id [ list-id ]
  (jdbc/with-connection schema/hsql-db
    (jdbc/with-query-results rows
      ["select * from todo_list where todo_list_id=?" list-id]
      (first rows))))

(defn get-friendly-users-by-id [ user-id ]
  (jdbc/with-connection schema/hsql-db
    (jdbc/with-query-results rows
      [(str "SELECT DISTINCT b.user_id, u.email_addr"
            "  FROM todo_list_owners a, todo_list_owners b, user u"
            " WHERE a.todo_list_id = b.todo_list_id"
            "   AND u.user_id = b.user_id"
            "   AND a.user_id = ?"
            " ORDER BY u.email_addr")
       user-id]
      (doall rows))))

(defn get-todo-list-owners-by-list-id [ list-id ]
  (jdbc/with-connection schema/hsql-db
    (jdbc/with-query-results rows
      [(str "SELECT user_id"
            "  FROM todo_list_owners"
            " WHERE todo_list_id=?")
       list-id]
      (doall (map :user_id rows)))))

(defn get-todo-list-ids-by-user [ user-id ]
  (jdbc/with-connection schema/hsql-db
    (jdbc/with-query-results rows
      [(str "SELECT DISTINCT todo_list_id"
            "  FROM todo_list_owners"
            "  WHERE user_id=?")
       user-id]
      (doall (map :todo_list_id rows)))))

(defn get-todo-lists-by-user [ user-id ]
  (jdbc/with-connection schema/hsql-db
    (jdbc/with-query-results rows
      [(str "SELECT DISTINCT todo_list.todo_list_id, todo_list.desc"
            "  FROM todo_list, todo_list_owners"
            " WHERE todo_list.todo_list_id=todo_list_owners.todo_list_id"
            "   AND todo_list_owners.user_id=?") user-id]
      (doall rows))))

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
        list-id (add-list "Todo")]
    (add-list-owner uid list-id)
    uid))

(defn add-todo-item [ todo-list-id desc ]
  (jdbc/with-connection schema/hsql-db
    (:item_id (first
               (jdbc/insert-records
                :todo_item
                {:todo_list_id todo-list-id
                 :desc desc
                 :created_on (java.util.Date.)})))))

(defn get-pending-items [ list-id ]
  (jdbc/with-connection schema/hsql-db
    (jdbc/with-query-results rows
      [(str "SELECT item.item_id, item.todo_list_id, item.desc, item.created_on"
            " FROM todo_item item" 
            " WHERE todo_list_id=?"
            "   AND NOT EXISTS (SELECT 1 FROM todo_item_completion WHERE item_id=item.item_id)")
       list-id ]
      (doall rows))))

(defn complete-item-by-id [ user-id item-id ]
  (jdbc/with-connection schema/hsql-db
    (jdbc/insert-records
     :todo_item_completion
     { :user_id user-id
      :item_id item-id
      :completed_on (java.util.Date.)})))

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

;; TODO: remove-item-by-id