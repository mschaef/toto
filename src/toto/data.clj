(ns toto.data
  (:use toto.util)
  (:require [clojure.java.jdbc :as jdbc]
            [sql-file.core :as sql-file]))

(def db-connection
  (delay (sql-file/open-hsqldb-file-conn "toto-db"  "toto" 0)))

(def ^:dynamic *db* nil)

(defmacro with-db-connection [ var & body ]
  `(binding [ *db* @db-connection ]
     ~@body))

(defn all-user-names [ ]
  (map :name (query-all *db* ["select name from user order by name"])))

(defn get-user-by-email [ email-addr ]
  (query-first *db* ["select * from user where email_addr=?" email-addr]))

(defn user-email-exists? [ email-addr ]
  (not (nil? (get-user-by-email email-addr))))

(defn get-user-by-id [ user-id ]
  (query-first *db* ["select * from user where user_id=?" user-id]))

(defn get-todo-list-by-id [ list-id ]
  (query-first *db* ["select * from todo_list where todo_list_id=?" list-id]))

(defn get-friendly-users-by-id [ user-id ]
  (query-all *db* [(str "SELECT DISTINCT b.user_id, u.email_addr"
                      "  FROM todo_list_owners a, todo_list_owners b, user u"
                      " WHERE a.todo_list_id = b.todo_list_id"
                      "   AND u.user_id = b.user_id"
                      "   AND a.user_id = ?"
                      " ORDER BY u.email_addr")
                 user-id]))

(defn get-todo-list-owners-by-list-id [ list-id ]
  (map :user_id  (query-all *db* [(str "SELECT user_id"
                                     "  FROM todo_list_owners"
                                     " WHERE todo_list_id=?")
                                list-id])))

(defn get-todo-list-ids-by-user [ user-id ]
  (map :todo_list_id (query-all *db* [(str "SELECT DISTINCT todo_list_id"
                                         "  FROM todo_list_owners"
                                         "  WHERE user_id=?")
                                    user-id])))

(defn get-todo-lists-by-user [ user-id ]
  (query-all *db* [(str "SELECT DISTINCT todo_list.todo_list_id,"
                      "                todo_list.desc,"
                      "                (SELECT count(item.item_id)"
                      "                   FROM todo_item item" 
                      "                  WHERE item.todo_list_id=todo_list.todo_list_id"
                      "                    AND NOT EXISTS (SELECT 1 FROM todo_item_completion WHERE item_id=item.item_id))"
                      "                   AS item_count"
                      "  FROM todo_list, todo_list_owners"
                      " WHERE todo_list.todo_list_id=todo_list_owners.todo_list_id"
                      "   AND todo_list_owners.user_id=?"
                      " ORDER BY todo_list.desc") user-id]))

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

(defn list-owned-by-user-id? [ list-id user-id ]
  (> (query-scalar *db* [(str "SELECT COUNT(*)"
                            "  FROM todo_list_owners"
                            " WHERE todo_list_id=?"
                            "   AND user_id=?")
                       list-id
                       user-id])
     0))

(defn item-owned-by-user-id? [ item-id user-id ]
  (> (query-scalar *db* [(str "SELECT COUNT(*)"
                            "  FROM todo_list_owners lo, todo_item item"
                            " WHERE item.item_id=?"
                            "   AND lo.todo_list_id=item.todo_list_id"
                            "   AND lo.user_id=?")
                       item-id
                       user-id])
     0))

(defn add-todo-item [ todo-list-id desc ]
  (:item_id (first
             (jdbc/insert! *db*
              :todo_item
              {:todo_list_id todo-list-id
               :desc desc
               :priority 0
               :created_on (java.util.Date.)}))))

(defn get-pending-items [ list-id completed-within-days ]
  (jdbc/query *db* [(str "SELECT item.item_id,"
                  "       item.todo_list_id,"
                  "       item.desc,"
                  "       item.created_on,"
                  "       item.priority,"
                  "       completion.completed_on,"
                  "       completion.is_delete,"
                  "       DATEDIFF('day', item.created_on, CURRENT_TIMESTAMP) as age_in_days"
                  " FROM todo_item item" 
                  "      LEFT JOIN todo_item_completion completion"
                  "        ON item.item_id = completion.item_id"
                  " WHERE item.todo_list_id = ?"
                  "   AND (completion.completed_on IS NULL "
                  "        OR  completion.completed_on > DATEADD('day', ?, CURRENT_TIMESTAMP))"
                  " ORDER BY item.priority DESC,"
                  "          item.item_id"
                  )
             list-id
             (- completed-within-days)]))

(defn get-pending-item-count [ list-id ]
  (query-scalar *db* [(str "SELECT count(item.item_id)"
                         " FROM todo_item item" 
                         " WHERE todo_list_id=?"
                         "   AND NOT EXISTS (SELECT 1 FROM todo_item_completion WHERE item_id=item.item_id)")
                    list-id]))

(defn empty-list? [ list-id ]
  (<= (get-pending-item-count list-id) 0))

(defn get-item-by-id [ item-id ]
  (query-first *db* [(str "SELECT item.item_id, item.todo_list_id, item.desc, item.created_on"
                        " FROM todo_item item" 
                        " WHERE item_id=?")
                   item-id]))


(defn is-item-completed? [ item-id ]
  (> (query-scalar *db* ["SELECT COUNT(*) FROM todo_item_completion WHERE item_id=?"
                   item-id ])
     0))

(defn complete-item-by-id [ user-id item-id ]
  (jdbc/with-db-transaction [ trans *db* ]
   (when (not (is-item-completed? item-id))
     (jdbc/insert! trans
      :todo_item_completion
      { :user_id user-id
       :item_id item-id
       :completed_on (java.util.Date.)
       :is_delete false}))))

(defn delete-item-by-id [ user-id item-id ]
  (jdbc/insert! *db*
   :todo_item_completion
   { :user_id user-id
    :item_id item-id
    :completed_on (java.util.Date.)
    :is_delete true}))

(defn restore-item [ item-id ]
  (jdbc/delete! *db*
   :todo_item_completion
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

(defn get-item-by-id [ item-id ]
  (query-first *db* ["select * from todo_item where item_id=?" item-id]))

;; TODO: remove-item-by-id
