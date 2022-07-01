;; Copyright (c) 2015-2022 Michael Schaeffer (dba East Coast Toolworks)
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
  (:use toto.core.util
        sql-file.sql-util
        sql-file.middleware)
  (:require [clojure.tools.logging :as log]
            [clojure.java.jdbc :as jdbc]
            [sql-file.core :as sql-file]
            [toto.data.queries :as query]))

;;; user

(defn get-user-roles [ user-id ]
  (set
   (map #(keyword "toto.role" (:role_name %))
        (query-all (current-db-connection)
                   [(str "SELECT role_name"
                         "  FROM user u, role r, user_role ur"
                         "  WHERE u.user_id = ur.user_id"
                         "    AND ur.role_id = r.role_id"
                         "    AND u.user_id = ?")
                    user-id]))))

(defn- get-role-id [ role-name ]
  (query-scalar (current-db-connection)
                [(str "SELECT role_id"
                      "  FROM role"
                      " WHERE role_name = ?")
                 (name role-name)]))

(defn delete-user-roles [ user-id ]
  (jdbc/delete! (current-db-connection) :user_role ["user_id=?" user-id]))


(defn set-user-roles [ user-id role-set ]
  (jdbc/with-db-transaction [ trans (current-db-connection) ]
    (delete-user-roles user-id)
    (doseq [ role-id (map get-role-id role-set)]
      (jdbc/insert! (current-db-connection) :user_role
                    {:user_id user-id
                     :role_id role-id}))))


(defn add-user-roles [ user-id role-set ]
  (set-user-roles user-id (clojure.set/union (get-user-roles user-id)
                                             role-set)))


(defn get-user-by-email [ email-addr ]
  (first
   (query/get-user-by-email { :email_addr email-addr}
                            { :connection (current-db-connection) })))

(defn user-email-exists? [ email-addr ]
  (not (nil? (get-user-by-email email-addr))))

(defn get-user-by-id [ user-id ]
  (first
   (query/get-user-by-id { :user_id user-id }
                         { :connection (current-db-connection) })))

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

(defn get-todo-lists-by-user [ user-id ]
  (query/get-todo-lists-by-user { :user_id user-id }
                                { :connection (current-db-connection) }))

(defn get-user-list-count [ user-id ]
  (count (get-todo-lists-by-user user-id)))

(defn add-user [ email-addr password ]
  (:user_id (first
             (jdbc/insert! (current-db-connection)
              :user
              {:email_addr email-addr
               :password password
               :account_created_on (current-time)
               :password_created_on (current-time)
               :friendly_name email-addr}))))

(defn set-user-password [ email-addr password ]
  (jdbc/update! (current-db-connection) :user
                {:password password
                 :password_created_on (current-time)}
                ["email_addr=?" email-addr]))

(defn set-user-name [ email-addr name ]
  (jdbc/update! (current-db-connection) :user
                {:friendly_name name}
                ["email_addr=?" email-addr]))

(defn record-user-login [ email-addr login-ip ]
  (jdbc/update! (current-db-connection) :user
                {:last_login_on (current-time)
                 :last_login_ip login-ip}
                ["email_addr=?" email-addr]))

(defn record-user-login-failure [ user-id request-ip ]
  (jdbc/insert! (current-db-connection) :login_failure
                {:user_id user-id
                 :failed_on (current-time)
                 :request_ip request-ip}))

(defn reset-login-failures [ user-id ]
  (jdbc/delete! (current-db-connection)
                :login_failure
                ["user_id=?" user-id]))

(defn create-verification-link [ user-id ]
  (:verification_link_id
   (first
    (jdbc/insert! (current-db-connection) :verification_link
                  {:link_uuid (.toString (java.util.UUID/randomUUID))
                   :verifies_user_id user-id
                   :created_on (current-time)}))))

(defn get-verification-link-by-user-id [ user-id ]
  (query-first (current-db-connection) [(str "SELECT *"
                          "  FROM verification_link"
                          " WHERE verifies_user_id=?")
                     user-id]))

(defn get-verification-link-by-id [ link-id ]
  (query-first (current-db-connection) [(str "SELECT *"
                          "  FROM verification_link"
                          " WHERE verification_link_id=?")
                     link-id]))

(defn get-verification-link-by-uuid [ link-uuid ]
  (query-first (current-db-connection) [(str "SELECT *"
                          "  FROM verification_link"
                          " WHERE link_uuid=?")
                     link-uuid]))

(defn delete-old-web-sessions []
  (jdbc/delete! (current-db-connection)
                :web_session
                ["accessed_on_day < DATEADD('month', -1, CURRENT_TIMESTAMP)"]))

(defn delete-old-verification-links []
  (jdbc/delete! (current-db-connection)
                :verification_link
                ["created_on < DATEADD('hour', -1, CURRENT_TIMESTAMP)"]))

(defn add-list [ desc is-view ]
  (:todo_list_id (first
                  (jdbc/insert! (current-db-connection)
                   :todo_list
                   {:desc desc
                    :is_view is-view}))))

(defn set-list-ownership [ todo-list-id user-ids ]
  (jdbc/with-db-transaction [ trans (current-db-connection) ]
    (let [next-owners (set user-ids)
          current-owners (set (get-todo-list-owners-by-list-id todo-list-id))
          add-ids (clojure.set/difference next-owners current-owners)
          remove-ids (clojure.set/difference current-owners next-owners)]
      (doseq [id remove-ids]
        (jdbc/delete! trans
                      :todo_list_owners
                      ["todo_list_id=? and user_id=?" todo-list-id id]))

      (doseq [ user-id add-ids ]
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

(defn update-list-description [ list-id list-description ]
  (jdbc/update! (current-db-connection)
   :todo_list
   {:desc list-description}
   ["todo_list_id=?" list-id]))

(defn set-list-public [ list-id public? ]
  (jdbc/update! (current-db-connection) :todo_list
                {:is_public public?}
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
  (- (scalar-result (query/get-min-ordinal-by-list { :list_id todo-list-id }
                                            { :connection (current-db-connection)})
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

(defn get-pending-items [ list-id completed-within-days snoozed-within-days ]
  (query/get-pending-items {:list_id list-id
                            :completed_within_days (- completed-within-days)
                            :snoozed_within_days snoozed-within-days}
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
  (let [current-items (set (map :desc  (get-pending-items list-id 0 0)))
        from-items (get-pending-items copy-from-list-id 0 0)]
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

(defn restore-item [ user-id item-id ]
  (update-item-by-id! user-id item-id
                      {:is_deleted false
                       :is_complete false}))

(defn update-item-snooze-by-id [ user-id item-id snoozed-until ]
  (update-item-by-id! user-id item-id
                      {:snoozed_until snoozed-until}))

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
