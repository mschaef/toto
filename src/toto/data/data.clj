;; Copyright (c) 2015-2023 Michael Schaeffer (dba East Coast Toolworks)
;;
;; Licensed as below.
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;       http://www.apache.org/licenses/LICENSE-2.0
;;
;; The license is also includes at the root of the project in the file
;; LICENSE.
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.
;;
;; You must not remove this notice, or any other, from this software.

(ns toto.data.data
  (:use playbook.core
        sql-file.sql-util
        sql-file.middleware)
  (:require [taoensso.timbre :as log]
            [clojure.java.jdbc :as jdbc]
            [sql-file.core :as sql-file]
            [toto.data.queries :as query]
            [base.data.data :as core-data]))

(defn get-todo-list-by-id [ list-id ]
  (first
   (query/get-todo-list-by-id { :todo_list_id list-id }
                              { :connection (current-db-connection) })))

(defn get-view-sublists [ user-id list-id ]
  (query/get-view-sublists { :user_id user-id
                             :todo_list_id list-id }
                           { :connection (current-db-connection) }))

(defn list-public? [ list-id ]
  (scalar-result
   (query/get-todo-list-is-public-by-id { :todo_list_id list-id }
                                        { :connection (current-db-connection) })))

(defn get-friendly-users-by-id [ user-id ]
  (query/get-friendly-users-by-id { :user_id user-id }
                                  { :connection (current-db-connection) }))

(defn get-todo-list-owners-by-list-id [ list-id ]
  (map :user_id
       (query/get-todo-list-owners-by-list-id { :todo_list_id list-id }
                                              { :connection (current-db-connection) })))


(defn get-todo-list-ids-by-user [ user-id ]
  (map :todo_list_id
       (query/get-todo-list-ids-by-user { :user_id user-id }
                                        { :connection (current-db-connection) })))

(defn get-todo-list-item-count [ todo-list-id include-snoozed ]
  (scalar-result
   (query/get-todo-list-item-count {:todo_list_id todo-list-id
                                    :include_snoozed include-snoozed}
                                   { :connection (current-db-connection) })))

(defn get-todo-lists-by-user [ user-id include-deleted ]
  (query/get-todo-lists-by-user { :user_id user-id
                                 :include_deleted include-deleted }
                                { :connection (current-db-connection) }))

(defn get-todo-lists-by-user-alphabetical [ user-id include-deleted ]
  (query/get-todo-lists-by-user-alphabetical { :user_id user-id
                                              :include_deleted include-deleted }
                                             { :connection (current-db-connection) }))

(defn get-todo-lists-with-item-age-limit [ ]
  (query/get-todo-lists-with-item-age-limit { }
                                            { :connection (current-db-connection) }))

(defn get-user-list-count [ user-id ]
  (count (get-todo-lists-by-user user-id false)))

(defn add-list [ desc is-view ]
  (:todo_list_id (first
                  (jdbc/insert! (current-db-connection)
                   :todo_list
                   {:desc desc
                    :is_view is-view}))))

(defn set-view-sublist-ids [ user-id todo-list-id sublist-ids ]
  (jdbc/with-db-transaction [ trans (current-db-connection) ]
    (let [next-sublists (set sublist-ids)
          current-sublists (set (map :sublist_id (get-view-sublists user-id todo-list-id)))
          add-ids (clojure.set/difference next-sublists current-sublists)
          remove-ids (clojure.set/difference current-sublists next-sublists)]
      (doseq [ list-id remove-ids ]
        (jdbc/delete! trans
                      :todo_view_sublist
                      ["todo_list_id=? and sublist_id=?" todo-list-id list-id]))
      (doseq [ list-id add-ids ]
        (jdbc/insert! trans
                      :todo_view_sublist
                      {:todo_list_id todo-list-id
                       :sublist_id list-id})))))

(defn- get-views-with-sublist [ user-id sublist-id ]
  (map :todo_list_id
       (query/get-views-with-sublist { :user_id user-id :sublist_id sublist-id}
                                     { :connection (current-db-connection) })))

(defn- delete-sublist-from-users-views [ trans user-id sublist-id ]
  (doseq [ containing-todo-list-id (get-views-with-sublist user-id sublist-id)]
    (jdbc/delete! trans
                  :todo_view_sublist
                  ["todo_list_id=? and sublist_id=?" containing-todo-list-id sublist-id])))

(defn set-list-ownership [ todo-list-id user-ids ]
  (jdbc/with-db-transaction [ trans (current-db-connection) ]
    (let [next-owners (set user-ids)
          current-owners (set (get-todo-list-owners-by-list-id todo-list-id))
          add-user-ids (clojure.set/difference next-owners current-owners)
          remove-user-ids (clojure.set/difference current-owners next-owners)]
      (doseq [ user-id remove-user-ids ]
        (delete-sublist-from-users-views trans user-id todo-list-id)
        (jdbc/delete! trans
                      :todo_list_owners
                      ["todo_list_id=? and user_id=?" todo-list-id user-id]))

      (doseq [ user-id add-user-ids ]
        (jdbc/insert! trans
                      :todo_list_owners
                      {:user_id user-id
                       :todo_list_id todo-list-id})))))

(defn set-list-priority [ todo-list-id user-id list-priority ]
  (jdbc/update! (current-db-connection) :todo_list_owners
                {:priority list-priority}
                ["todo_list_id=? and user_id=?" todo-list-id user-id]))

(defn remove-list-owner [ todo-list-id user-id ]
  (jdbc/delete! (current-db-connection) :todo_list_owners
                ["todo_list_id=? and user_id=?" todo-list-id user-id]))

(defn delete-list [ todo-list-id ]
  (jdbc/update! (current-db-connection) :todo_list
                {:is_deleted true}
                ["todo_list_id=?" todo-list-id]))

(defn restore-list [ todo-list-id ]
  (jdbc/update! (current-db-connection) :todo_list
                {:is_deleted false}
                ["todo_list_id=?" todo-list-id]))

(defn update-list-description [ list-id list-description ]
  (jdbc/update! (current-db-connection)
   :todo_list
   {:desc list-description}
   ["todo_list_id=?" list-id]))

(defn set-list-public [ list-id public? ]
  (jdbc/update! (current-db-connection) :todo_list
                {:is_public public?}
                ["todo_list_id=?" list-id]))

(defn set-list-max-item-age [ list-id max-item-age ]
  (jdbc/update! (current-db-connection) :todo_list
                {:max_item_age max-item-age}
                ["todo_list_id=?" list-id]))

(defn clear-list-max-item-age [ list-id ]
  (jdbc/update! (current-db-connection) :todo_list
                {:max_item_age nil}
                ["todo_list_id=?" list-id]))

(defn list-owned-by-user-id? [ list-id user-id ]
  (> (scalar-result
      (query/list-owned-by-user-id? { :list_id list-id :user_id user-id }
                                    { :connection (current-db-connection) }))
     0))

(defn item-owned-by-user-id? [ item-id user-id ]
  (> (scalar-result
      (query/item-owned-by-user-id? { :item_id item-id :user_id user-id }
                                    { :connection (current-db-connection) }))
     0))

(defn get-next-list-ordinal [ todo-list-id ]
  (-  (or (scalar-result
           (query/get-min-ordinal-by-list { :list_id todo-list-id }
                                          { :connection (current-db-connection)})
           0)
          0)
      1))

(defn add-todo-item [ user-id todo-list-id desc priority ]
  (:item_id (first
             (jdbc/insert! (current-db-connection)
                           :todo_item
                           {:todo_list_id todo-list-id
                            :desc desc
                            :created_on (current-time)
                            :created_by user-id
                            :priority priority
                            :updated_by user-id
                            :updated_on (current-time)
                            :is_deleted false
                            :is_complete false
                            :item_ordinal (get-next-list-ordinal todo-list-id)}))))

(defn get-item-count [ list-id ]
  (scalar-result
   (query/get-item-count {:list_id list-id }
                         { :connection (current-db-connection) })))

(defn get-pending-items [ list-id completed-within-days snoozed-within-days min-item-priority ]
  (query/get-pending-items {:list_id list-id
                            :completed_within_days (- completed-within-days)
                            :snoozed_within_days snoozed-within-days
                            :min_item_priority min-item-priority}
                           { :connection (current-db-connection) }))

(defn get-completed-items [ list-id completed-within-days ]
  (query/get-completed-items {:list_id list-id
                              :completed_within_days (- completed-within-days)}
                           { :connection (current-db-connection) }))

(defn get-pending-item-order-by-description [ list-id ]
  (query/get-pending-item-order-by-description {:list_id list-id}
                                               { :connection (current-db-connection) }))

(defn get-pending-item-order-by-created-on [ list-id ]
  (query/get-pending-item-order-by-created-on {:list_id list-id}
                                               { :connection (current-db-connection) }))

(defn get-pending-item-order-by-updated-on [ list-id ]
  (query/get-pending-item-order-by-updated-on {:list_id list-id}
                                               { :connection (current-db-connection) }))

(defn get-pending-item-order-by-snoozed-until [ list-id ]
  (query/get-pending-item-order-by-snoozed-until {:list_id list-id}
                                               { :connection (current-db-connection) }))


(defn get-pending-item-count [ list-id ]
  (scalar-result
   (query/get-pending-item-count { :list_id list-id }
                                 { :connection (current-db-connection) })))

(defn empty-list? [ list-id ]
  (<= (get-pending-item-count list-id) 0))

(defn get-item-by-id [ item-id ]
  (first
   (query/get-item-by-id { :item_id item-id }
                         { :connection (current-db-connection) })))


(defn update-item-ordinal! [ item-id new-ordinal ]
  ;; Ordinal changes not audited on the theory they happen so often
  (jdbc/update! (current-db-connection)
                :todo_item
                {:item_ordinal new-ordinal}
                ["item_id=?" item-id]))

(defn shift-list-items! [ todo-list-id begin-ordinal ]
  (doseq [item (query/list-items-tail {:todo_list_id todo-list-id
                                       :begin_ordinal begin-ordinal}
                                      { :connection (current-db-connection) })]
    (update-item-ordinal! (:item_id item) (+ 1 (:item_ordinal item)))))

(defn set-items-order! [ item-ids ]
  (doseq [ [ item-id item-ordinal ] (sequence (map vector) item-ids (range)) ]
    (update-item-ordinal! item-id item-ordinal)))

(defn order-list-items-by-description! [ list-id ]
  (set-items-order!
   (map :item_id (get-pending-item-order-by-description list-id))))

(defn order-list-items-by-created-on! [ list-id ]
  (set-items-order!
   (map :item_id (get-pending-item-order-by-created-on list-id))))

(defn order-list-items-by-updated-on! [ list-id ]
  (set-items-order!
   (map :item_id (get-pending-item-order-by-updated-on list-id))))

(defn order-list-items-by-snoozed-until! [ list-id ]
  (set-items-order!
   (map :item_id (get-pending-item-order-by-snoozed-until list-id))))

(defn copy-list [ user-id list-id copy-from-list-id ]
  (let [current-items (set (map :desc  (get-pending-items list-id 0 0 -99999)))
        from-items (get-pending-items copy-from-list-id 0 0 -99999)]
    (doseq [ new-item (remove
        #(current-items (:desc %))
        from-items)]
      (add-todo-item user-id list-id (:desc new-item) (:priority new-item)))))

(defn update-item-by-id! [ user-id item-id values ]
  (jdbc/with-db-transaction [ trans (current-db-connection) ]
    (jdbc/insert! trans :todo_item_history
                  (query-first (current-db-connection) [(str "SELECT *"
                                          "  FROM todo_item"
                                          " WHERE item_id=?")
                                     item-id]))
    (jdbc/update! trans
                  :todo_item
                  (merge
                   values
                   {:updated_by user-id
                    :updated_on (current-time)})
                  ["item_id=?" item-id])))

(defn complete-item-by-id [ user-id item-id ]
  (update-item-by-id! user-id item-id {:is_complete true}))

(defn delete-item-by-id [ user-id item-id ]
  (update-item-by-id! user-id item-id {:is_deleted true}))

(defn get-system-user-id []
  (:user_id (core-data/get-user-by-email "toto@mschaef.com")))

(defn get-sunset-items-by-id [ list-id age-limit ]
  (query/get-sunset-items-by-id {:todo_list_id list-id
                                 :age_limit age-limit}
                                { :connection (current-db-connection) }))

(defn restore-item [ user-id item-id ]
  (update-item-by-id! user-id item-id
                      {:is_deleted false
                       :is_complete false}))

(defn update-item-snooze-by-id [ user-id item-id snoozed-until ]
  (update-item-by-id! user-id item-id
                      {:snoozed_until snoozed-until}))

(defn remove-item-snooze-by-id [ user-id item-id ]
  (update-item-by-id! user-id item-id
                      {:snoozed_until nil}))

(defn update-item-desc-by-id [ user-id item-id item-description ]
  (update-item-by-id! user-id item-id
                      {:desc item-description}))

(defn update-item-priority-by-id [ user-id item-id item-priority ]
  (update-item-by-id! user-id item-id
                      {:priority item-priority}))

(defn update-item-list [ user-id item-id list-id ]
  (update-item-by-id! user-id item-id
                      {:todo_list_id list-id}))

(defn get-list-id-by-item-id [ item-id ]
  (scalar-result
   (query/get-list-id-by-item-id { :item_id item-id }
                                 { :connection (current-db-connection) })))
