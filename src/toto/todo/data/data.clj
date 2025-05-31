;; Copyright (c) 2015-2025 Michael Schaeffer (dba East Coast Toolworks)
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

(ns toto.todo.data.data
  (:use playbook.core
        sql-file.sql-util
        sql-file.middleware)
  (:require [taoensso.timbre :as log]
            [clojure.java.jdbc :as jdbc]
            [sql-file.core :as sql-file]
            [toto.todo.data.queries :as query]
            [toto.data.data :as core-data]))

(defn get-todo-list-by-id [list-id]
  (first
   (query/get-todo-list-by-id {:todo_list_id list-id})))

(defn get-view-sublists [user-id list-id]
  (query/get-view-sublists {:user_id user-id
                            :todo_list_id list-id}))

(defn list-public? [list-id]
  (scalar-result
   (query/get-todo-list-is-public-by-id {:todo_list_id list-id})))

(defn get-friendly-users-by-id [user-id]
  (query/get-friendly-users-by-id {:user_id user-id}))

(defn get-todo-list-owners-by-list-id [list-id]
  (map :user_id
       (query/get-todo-list-owners-by-list-id {:todo_list_id list-id})))

(defn get-todo-list-ids-by-user [user-id]
  (map :todo_list_id
       (query/get-todo-list-ids-by-user {:user_id user-id
                                         :include_deleted false})))

(defn get-all-todo-list-ids-by-user [user-id]
  (map :todo_list_id
       (query/get-todo-list-ids-by-user {:user_id user-id
                                         :include_deleted true})))

(defn get-todo-list-view-item-count
  ([todo-list-view-id include-snoozed minimum-priority]
   (scalar-result
    (query/get-todo-list-view-item-count {:todo_list_view_id todo-list-view-id
                                          :include_snoozed include-snoozed
                                          :minimum_priority minimum-priority})))

  ([todo-list-view-id include-snoozed]
   (get-todo-list-view-item-count todo-list-view-id include-snoozed -9999)))

(defn get-todo-list-item-count
  ([todo-list-id include-snoozed minimum-priority]
   (scalar-result
    (query/get-todo-list-item-count {:todo_list_id todo-list-id
                                     :include_snoozed include-snoozed
                                     :minimum_priority minimum-priority})))

  ([todo-list-id include-snoozed]
   (get-todo-list-item-count todo-list-id include-snoozed -9999)))

(defn get-todo-lists-by-user [user-id include-deleted]
  (query/get-todo-lists-by-user {:user_id user-id
                                 :include_deleted include-deleted}))

(defn get-todo-list-owner-count [todo-list-id]
  (scalar-result
   (query/get-todo-list-owner-count {:todo_list_id todo-list-id})))

(defn get-todo-lists-by-user-alphabetical [user-id include-deleted]
  (query/get-todo-lists-by-user-alphabetical {:user_id user-id
                                              :include_deleted include-deleted}))

(defn get-todo-lists-with-item-age-limit []
  (query/get-todo-lists-with-item-age-limit {}))

(defn get-user-list-count [user-id]
  (count (get-todo-lists-by-user user-id false)))

(defn add-list [desc is-view]
  (:todo_list_id (query/add-list<! {:desc desc
                                    :is_view is-view})))

(defn set-view-sublist-ids [user-id todo-list-id sublist-ids]
  (with-db-transaction
    (let [next-sublists (set sublist-ids)
          current-sublists (set (map :sublist_id (get-view-sublists user-id todo-list-id)))
          add-sublist-ids (clojure.set/difference next-sublists current-sublists)
          remove-sublist-ids (clojure.set/difference current-sublists next-sublists)]
      (doseq [sublist-id remove-sublist-ids]
        (query/delete-view-sublist! {:todo_list_id todo-list-id
                                     :sublist_id sublist-id}))
      (doseq [sublist-id add-sublist-ids]
        (query/insert-view-sublist! {:todo_list_id todo-list-id
                                     :sublist_id sublist-id})))))

(defn- get-views-with-sublist [user-id sublist-id]
  (map :todo_list_id
       (query/get-views-with-sublist {:user_id user-id :sublist_id sublist-id})))

(defn- delete-sublist-from-users-views [user-id sublist-id]
  (doseq [containing-todo-list-id (get-views-with-sublist user-id sublist-id)]
    (query/delete-view-sublist! {:todo_list_id containing-todo-list-id
                                 :sublist_id sublist-id})))

(defn delete-list-owner! [todo-list-id user-id]
  (query/delete-list-owner!  {:todo_list_id todo-list-id
                              :user_id user-id}))

(defn insert-list-owner! [todo-list-id user-id]
  (query/insert-list-owner! {:todo_list_id todo-list-id
                             :user_id user-id}))

(defn set-list-ownership [todo-list-id user-ids]
  (with-db-transaction
    (let [next-owners (set user-ids)
          current-owners (set (get-todo-list-owners-by-list-id todo-list-id))
          add-user-ids (clojure.set/difference next-owners current-owners)
          remove-user-ids (clojure.set/difference current-owners next-owners)]
      (doseq [user-id remove-user-ids]
        (delete-sublist-from-users-views user-id todo-list-id)
        (delete-list-owner! todo-list-id user-id))
      (doseq [user-id add-user-ids]
        (insert-list-owner! todo-list-id user-id)))))

(defn get-list-priority [todo-list-id user-id]
  (scalar-result
   (query/get-list-priority {:todo_list_id todo-list-id
                             :user_id user-id})))

(defn set-list-priority [todo-list-id user-id list-priority]
  (query/set-list-priority! {:todo_list_id todo-list-id
                             :user_id user-id
                             :priority list-priority}))

(defn delete-list [todo-list-id]
  (query/set-list-deleted! {:todo_list_id todo-list-id
                            :is_deleted true}))

(defn obliterate-list! [todo-list-id]
  (log/warn "Obliterating list: " todo-list-id)
  (query/obliterate-list! {:todo_list_id todo-list-id}))

(defn restore-list [todo-list-id]
  (query/set-list-deleted! {:todo_list_id todo-list-id
                            :is_deleted false}))

(defn update-list-description [todo-list-id description]
  (query/set-list-description! {:todo_list_id todo-list-id
                                :desc description}))

(defn set-list-public [todo-list-id is-public?]
  (query/set-list-public! {:todo_list_id todo-list-id
                           :is_public is-public?}))

(defn set-list-max-item-age [todo-list-id max-item-age]
  (query/set-list-max-item-age! {:todo_list_id todo-list-id
                                 :max_item_age max-item-age}))

(defn clear-list-max-item-age [todo-list-id]
  (query/set-list-max-item-age! {:todo_list_id todo-list-id
                                 :max_item_age nil}))

(defn list-owned-by-user-id? [list-id user-id]
  (> (scalar-result
      (query/list-owned-by-user-id? {:list_id list-id :user_id user-id}))
     0))

(defn item-owned-by-user-id? [item-id user-id]
  (> (scalar-result
      (query/item-owned-by-user-id? {:item_id item-id :user_id user-id}))
     0))

(defn get-next-list-ordinal [todo-list-id]
  (- (or (scalar-result
          (query/get-min-ordinal-by-list {:list_id todo-list-id})
          0)
         0)
     1))

(defn add-todo-item [user-id todo-list-id desc priority]
  (:item_id (query/add-item<! {:todo_list_id todo-list-id
                               :desc desc
                               :created_on (current-time)
                               :created_by user-id
                               :priority priority
                               :updated_by user-id
                               :updated_on (current-time)
                               :is_deleted false
                               :is_complete false
                               :item_ordinal (get-next-list-ordinal todo-list-id)})))

(defn get-item-count [list-id]
  (scalar-result
   (query/get-item-count {:list_id list-id})))

(defn get-total-active-item-count []
  (scalar-result
   (query/get-total-active-item-count {})))

(defn get-total-item-history-count []
  (scalar-result
   (query/get-total-item-history-count {})))

(defn get-user-count []
  (scalar-result
   (query/get-user-count {})))

(defn get-pending-items [list-id completed-within-days snoozed-within-days min-item-priority]
  (query/get-pending-items {:list_id list-id
                            :completed_within_days (- completed-within-days)
                            :snoozed_within_days snoozed-within-days
                            :min_item_priority min-item-priority}))

(defn get-completed-items [list-id completed-within-days]
  (query/get-completed-items {:list_id list-id
                              :completed_within_days (- completed-within-days)}))

(defn get-pending-item-order-by-description [list-id]
  (query/get-pending-item-order-by-description {:list_id list-id}))

(defn get-pending-item-order-by-created-on [list-id]
  (query/get-pending-item-order-by-created-on {:list_id list-id}))

(defn get-pending-item-order-by-updated-on [list-id]
  (query/get-pending-item-order-by-updated-on {:list_id list-id}))

(defn get-pending-item-order-by-snoozed-until [list-id]
  (query/get-pending-item-order-by-snoozed-until {:list_id list-id}))

(defn get-pending-item-count [list-id]
  (scalar-result
   (query/get-pending-item-count {:list_id list-id})))

(defn empty-list? [list-id]
  (<= (get-pending-item-count list-id) 0))

(defn get-item-by-id [item-id]
  (first
   (query/get-item-by-id {:item_id item-id})))

(defn update-item-ordinal! [item-id new-ordinal]
  ;; Ordinal changes not audited on the theory they happen so often
  (query/set-item-ordinal! {:item_ordinal new-ordinal
                            :item_id item-id}))

(defn shift-list-items! [todo-list-id begin-ordinal]
  (doseq [item (query/list-items-tail {:todo_list_id todo-list-id
                                       :begin_ordinal begin-ordinal})]
    (update-item-ordinal! (:item_id item) (+ 1 (:item_ordinal item)))))

(defn set-items-order! [item-ids]
  (doseq [[item-id item-ordinal] (sequence (map vector) item-ids (range))]
    (update-item-ordinal! item-id item-ordinal)))

(defn order-list-items-by-description! [list-id]
  (set-items-order!
   (map :item_id (get-pending-item-order-by-description list-id))))

(defn order-list-items-by-created-on! [list-id]
  (set-items-order!
   (map :item_id (get-pending-item-order-by-created-on list-id))))

(defn order-list-items-by-updated-on! [list-id]
  (set-items-order!
   (map :item_id (get-pending-item-order-by-updated-on list-id))))

(defn order-list-items-by-snoozed-until! [list-id]
  (set-items-order!
   (map :item_id (get-pending-item-order-by-snoozed-until list-id))))

(defn copy-list [user-id list-id copy-from-list-id]
  (let [current-items (set (map :desc  (get-pending-items list-id 0 0 -99999)))
        from-items (get-pending-items copy-from-list-id 0 0 -99999)]
    (doseq [new-item (remove
                      #(current-items (:desc %))
                      from-items)]
      (add-todo-item user-id list-id (:desc new-item) (:priority new-item)))))

(defn update-item-by-id! [user-id item-id values]
  (with-db-transaction
    (jdbc/insert! (current-db-connection)
                  :todo_item_history
                  (first
                   (query/get-todo-item-by-id {:item_id item-id})))
    (jdbc/update! (current-db-connection)
                  :todo_item
                  (merge
                   values
                   {:updated_by user-id
                    :updated_on (current-time)})
                  ["item_id=?" item-id])))

(defn complete-item-by-id [user-id item-id]
  (update-item-by-id! user-id item-id {:is_complete true}))

(defn delete-item-by-id [user-id item-id]
  (update-item-by-id! user-id item-id {:is_deleted true}))

(defn get-system-user-id []
  (:user_id (core-data/get-user-by-email "toto@mschaef.com")))

(defn get-sunset-items-by-id [list-id age-limit]
  (query/get-sunset-items-by-id {:todo_list_id list-id
                                 :age_limit age-limit}))

(defn restore-item [user-id item-id]
  (update-item-by-id! user-id item-id
                      {:is_deleted false
                       :is_complete false}))

(defn update-item-snooze-by-id [user-id item-id snoozed-until]
  (update-item-by-id! user-id item-id
                      {:snoozed_until snoozed-until}))

(defn remove-item-snooze-by-id [user-id item-id]
  (update-item-by-id! user-id item-id
                      {:snoozed_until nil}))

(defn update-item-desc-by-id [user-id item-id item-description]
  (update-item-by-id! user-id item-id
                      {:desc item-description}))

(defn update-item-priority-by-id [user-id item-id item-priority]
  (update-item-by-id! user-id item-id
                      {:priority item-priority}))

(defn update-item-list [user-id item-id list-id]
  (update-item-by-id! user-id item-id
                      {:todo_list_id list-id}))

(defn get-list-id-by-item-id [item-id]
  (scalar-result
   (query/get-list-id-by-item-id {:item_id item-id})))

(defn reset-view-stars [user-id list-id]
  (doseq [item (query/get-todo-list-view-starred-item-ids {:todo_list_view_id list-id})]
    (update-item-priority-by-id user-id (:item_id item) 0)))

(defn reset-list-stars [user-id list-id]
  (doseq [item (query/get-todo-list-starred-item-ids {:todo_list_id list-id})]
    (update-item-priority-by-id user-id (:item_id item) 0)))
