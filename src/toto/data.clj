(ns toto.data
  (:use toto.util)
  (:require [clojure.java.jdbc :as jdbc]))

(defn query-all [ query-spec ]
  (jdbc/with-query-results rows
    query-spec
    (doall rows)))

(defn query-first [ query-spec ]
  (jdbc/with-query-results rows
    query-spec
    (first rows)))

(defn query-count [ query-spec ]
  (:c1 (query-first query-spec)))

(defn all-user-names []
  (map :name (query-all ["select name from user order by name"])))

(defn get-user-by-email [ email-addr ]
  (query-first ["select * from user where email_addr=?" email-addr]))

(defn user-email-exists? [ email-addr ]
  (not (nil? (get-user-by-email email-addr))))

(defn get-user-by-id [ user-id ]
  (query-first ["select * from user where user_id=?" user-id]))

(defn get-todo-list-by-id [ list-id ]
  (query-first ["select * from todo_list where todo_list_id=?" list-id]))

(defn get-friendly-users-by-id [ user-id ]
  (query-all [(str "SELECT DISTINCT b.user_id, u.email_addr"
                   "  FROM todo_list_owners a, todo_list_owners b, user u"
                   " WHERE a.todo_list_id = b.todo_list_id"
                   "   AND u.user_id = b.user_id"
                   "   AND a.user_id = ?"
                   " ORDER BY u.email_addr")
              user-id]))

(defn get-todo-list-owners-by-list-id [ list-id ]
  (map :user_id  (query-all [(str "SELECT user_id"
                                  "  FROM todo_list_owners"
                                  " WHERE todo_list_id=?")
                             list-id])))

(defn get-todo-list-ids-by-user [ user-id ]
  (map :todo_list_id (query-all [(str "SELECT DISTINCT todo_list_id"
                                      "  FROM todo_list_owners"
                                      "  WHERE user_id=?")
                                 user-id])))

(defn get-todo-lists-by-user [ user-id ]
  (query-all [(str "SELECT DISTINCT todo_list.todo_list_id,"
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
             (jdbc/insert-records
              :user
              {:email_addr email-addr
               :password password}))))

(defn add-list [ desc ]
  (:todo_list_id (first
                  (jdbc/insert-records
                   :todo_list
                   {:desc desc}))))

(defn add-list-owner [ user-id todo-list-id ]
  (:todo_list_id (first
                  (jdbc/insert-records
                   :todo_list_owners
                   {:user_id user-id
                    :todo_list_id todo-list-id}))))

(defn set-list-ownership [ todo-list-id user-ids ]
  (jdbc/transaction
   (jdbc/delete-rows
    :todo_list_owners
    ["todo_list_id=?" todo-list-id])

   (doseq [ user-id user-ids ]
     (jdbc/insert-records
      :todo_list_owners
      {:user_id user-id
       :todo_list_id todo-list-id}))))

(defn remove-list-owner [ todo-list-id user-id ]
  (jdbc/delete-rows
   :todo_list_owners
   ["todo_list_id=? and user_id=?" 
    todo-list-id
    user-id]))

(defn delete-list [ todo-list-id ]
  (jdbc/delete-rows
   :todo_list_owners
   ["todo_list_id=?" 
    todo-list-id]))

(defn update-list-description [ list-id list-description ]
  (jdbc/update-values
   :todo_list
   ["todo_list_id=?" list-id]
   {:desc list-description}))

(defn list-owned-by-user-id? [ list-id user-id ]
  (> (query-count [(str "SELECT COUNT(*)"
                        "  FROM todo_list_owners"
                        " WHERE todo_list_id=?"
                        "   AND user_id=?")
                   list-id
                   user-id])
     0))

(defn item-owned-by-user-id? [ item-id user-id ]
  (> (query-count [(str "SELECT COUNT(*)"
                        "  FROM todo_list_owners lo, todo_item item"
                        " WHERE item.item_id=?"
                        "   AND lo.todo_list_id=item.todo_list_id"
                        "   AND lo.user_id=?")
                   item-id
                   user-id])
     0))

(defn add-todo-item [ todo-list-id desc ]
  (:item_id (first
             (jdbc/insert-records
              :todo_item
              {:todo_list_id todo-list-id
               :desc desc
               :created_on (java.util.Date.)}))))

(defn get-pending-items [ list-id ]
  (query-all [(str "SELECT item.item_id,"
                   "       item.todo_list_id,"
                   "       item.desc,"
                   "       item.created_on,"
                   "       DATEDIFF('day', item.created_on, current_timestamp) as age_in_days"
                   " FROM todo_item item" 
                   " WHERE todo_list_id=?"
                   "   AND NOT EXISTS (SELECT 1 FROM todo_item_completion WHERE item_id=item.item_id)"
                   " ORDER BY item.item_id")
              list-id]))

(defn get-pending-item-count [ list-id ]
  (query-count [(str "SELECT count(item.item_id)"
                     " FROM todo_item item" 
                     " WHERE todo_list_id=?"
                     "   AND NOT EXISTS (SELECT 1 FROM todo_item_completion WHERE item_id=item.item_id)")
                list-id]))

(defn empty-list? [ list-id ]
  (<= (get-pending-item-count list-id) 0))

(defn get-item-by-id [ item-id ]
  (query-first [(str "SELECT item.item_id, item.todo_list_id, item.desc, item.created_on"
                     " FROM todo_item item" 
                     " WHERE item_id=?")
                item-id]))


(defn is-item-completed? [ item-id ]
  (> (query-count ["SELECT COUNT(*) FROM todo_item_completion WHERE item_id=?"
                   item-id ])
     0))

(defn complete-item-by-id [ user-id item-id ]
  (jdbc/transaction
   (when (not (is-item-completed? item-id))
     (jdbc/insert-records
      :todo_item_completion
      { :user_id user-id
       :item_id item-id
       :completed_on (java.util.Date.)
       :is_delete false}))))

(defn delete-item-by-id [ user-id item-id ]
  (jdbc/insert-records
   :todo_item_completion
   { :user_id user-id
    :item_id item-id
    :completed_on (java.util.Date.)
    :is_delete true}))

(defn update-item-by-id [ item-id item-description ]
  (jdbc/update-values
   :todo_item
   ["item_id=?" item-id]
   {:desc item-description}))

(defn update-item-list [ item-id list-id ]
  (jdbc/update-values
   :todo_item
   ["item_id=?" item-id]
   {:todo_list_id list-id}))

(defn get-item-by-id [ item-id ]
  (query-first ["select * from todo_item where item_id=?" item-id]))

;; TODO: remove-item-by-id