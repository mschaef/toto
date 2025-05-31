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

(ns toto.todo.user
  (:use playbook.core
        sql-file.middleware)
  (:require [taoensso.timbre :as log]
            [playbook.config :as config]
            [toto.data.data :as data]
            [toto.todo.data.data :as todo-data]))

(defn delete-unverified-user-job []
  (doseq [user-id (data/get-unverified-users-by-id (config/cval :max-unverified-user-age))]
    (log/info "Deleting unverified user: " user-id)
    (with-db-transaction
      (doseq [list-id  (todo-data/get-all-todo-list-ids-by-user user-id)]
        (todo-data/delete-list-owner! list-id user-id)
        (when (= 0 (todo-data/get-todo-list-owner-count list-id))
          (todo-data/obliterate-list! list-id)))
      (data/delete-user-by-id! user-id))))

