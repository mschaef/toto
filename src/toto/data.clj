(ns toto.data
  (:use toto.util
        sql-file.sql-util)
  (:require [clojure.java.jdbc :as jdbc]
            [sql-file.core :as sql-file]
            [toto.queries :as query]))

(def db-connection
  (delay (sql-file/open-sql-file
          (assoc (sql-file/hsqldb-file-conn (config-property "db.subname" "toto-db"))
                 :schema-path [ "sql/" ])
          [ "toto" 2 ])))


(def ^:dynamic *db* nil)

(defmacro with-db-connection [ var & body ]
  `(binding [ *db* @db-connection ]
     ~@body))

(defn- scalar [ query-result ]
  (let [first-row (first query-result)]
    (get first-row (first (keys first-row)))))

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

(defn get-todo-list-by-id [ list-id ]
  (first
   (query/get-todo-list-by-id { :todo_list_id list-id }
                              { :connection *db* })))

(defn list-public? [ list-id ]
  (scalar
   (query/get-todo-list-is-public-by-id { :todo_list_id list-id }
                                        { :connection *db* })))

(defn get-friendly-users-by-id [ user-id ]
  (query/get-friendly-users-by-id { :user_id user-id }
                                  { :connection *db* }))

(defn get-todo-list-owners-by-list-id [ list-id ]
  (map :user_id
       (query/get-todo-list-owners-by-list-id { :todo_list_id list-id }
                                              { :connection *db* })))


(defn get-todo-list-ids-by-user [ user-id ]
  (map :todo_list_id
       (query/get-todo-list-ids-by-user { :user_id user-id }
                                        { :connection *db* })))

(defn get-todo-lists-by-user [ user-id ]
  (query/get-todo-lists-by-user { :user_id user-id }
                                { :connection *db* }))

(defn add-user [ email-addr password ]
  (:user_id (first
             (jdbc/insert! *db*
              :user
              {:email_addr email-addr
               :password password}))))

(defn add-list [ desc ]
  (:todo_list_id (first
                  (jdbc/insert! *db*
                   :todo_list
                   {:desc desc}))))

(defn add-list-owner [ user-id todo-list-id ]
  (:todo_list_id (first
                  (jdbc/insert! *db*
                   :todo_list_owners
                   {:user_id user-id
                    :todo_list_id todo-list-id}))))

(defn set-list-ownership [ todo-list-id user-ids ]
  (jdbc/with-db-transaction [ trans *db* ] 
   (jdbc/delete! trans
    :todo_list_owners
    ["todo_list_id=?" todo-list-id])

   (doseq [ user-id user-ids ]
     (jdbc/insert! trans
      :todo_list_owners
      {:user_id user-id
       :todo_list_id todo-list-id}))))

(defn remove-list-owner [ todo-list-id user-id ]
  (jdbc/delete! *db*
   :todo_list_owners
   ["todo_list_id=? and user_id=?" 
    todo-list-id
    user-id]))

(defn delete-list [ todo-list-id ]
  (jdbc/delete! *db*
   :todo_list_owners
   ["todo_list_id=?" 
    todo-list-id]))

(defn update-list-description [ list-id list-description ]
  (jdbc/update! *db*
   :todo_list
   {:desc list-description}
   ["todo_list_id=?" list-id]))

(defn set-list-public [ list-id public? ]
  (jdbc/update! *db*
   :todo_list
   {:is_public public?}
   ["todo_list_id=?" list-id]))

(defn list-owned-by-user-id? [ list-id user-id ]
  (> (scalar
      (query/list-owned-by-user-id? { :list_id list-id :user_id user-id }
                                    { :connection *db* }))
     0))

(defn item-owned-by-user-id? [ item-id user-id ]
  (> (scalar
      (query/item-owned-by-user-id? { :item_id item-id :user_id user-id }
                                    { :connection *db* }))
     0))

(defn add-todo-item [ todo-list-id desc ]
  (:item_id (first
             (jdbc/insert! *db*
              :todo_item
              {:todo_list_id todo-list-id
               :desc desc
               :priority 0
               :created_on (java.util.Date.)}))))

(defn get-pending-items [ list-id completed-within-days snoozed-for-days ]
  (query/get-pending-items {:list_id list-id
                            :completed_within_days (- completed-within-days)
                            :snoozed_for_days snoozed-for-days}
                           { :connection *db* }))

(defn get-pending-item-count [ list-id ]
  (scalar
   (query/get-pending-item-count { :list_id list-id }
                                 { :connection *db* })))

(defn empty-list? [ list-id ]
  (<= (get-pending-item-count list-id) 0))

(defn get-item-by-id [ item-id ]
  (first
   (query/get-item-by-id { :item_id item-id }
                         { :Connection *db* })))


(defn is-item-completed? [ item-id ]
  (> (scalar
      (query/item-completion-count { :item_id item-id }
                                   { :connection *db* }))
     0))

(defn complete-item-by-id [ user-id item-id ]
  (query/set-item-completion! {:user_id user-id
                               :item_id item-id
                               :completed_on (java.util.Date.)
                               :is_delete false}
                              {:connection *db*}))

(defn delete-item-by-id [ user-id item-id ]
  (query/set-item-completion! {:user_id user-id
                               :item_id item-id
                               :completed_on (java.util.Date.)
                               :is_delete true}
                              {:connection *db*}))

(defn restore-item [ item-id ]
  (jdbc/delete! *db*
   :todo_item_completion
   ["item_id=?" item-id]))

(defn update-item-snooze-by-id [ item-id snoozed-until ]
  (jdbc/update! *db*
   :todo_item
   {:snoozed_until snoozed-until}
   ["item_id=?" item-id]))

(defn update-item-desc-by-id [ item-id item-description ]
  (jdbc/update! *db*
   :todo_item
   {:desc item-description}
   ["item_id=?" item-id]))

(defn update-item-priority-by-id [ item-id item-priority ]
  (jdbc/update! *db*
   :todo_item
   {:priority item-priority}
   ["item_id=?" item-id]))

(defn update-item-list [ item-id list-id ]
  (jdbc/update! *db*
   :todo_item
   {:todo_list_id list-id}
   ["item_id=?" item-id]))

(defn get-list-id-by-item-id [ item-id ]
  (scalar
   (query/get-list-id-by-item-id { :item_id item-id }
                                 { :connection *db* })))
