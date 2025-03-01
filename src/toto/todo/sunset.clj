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

(ns toto.todo.sunset
  (:use playbook.core
        compojure.core)
  (:require [taoensso.timbre :as log]
            [toto.todo.data.data :as data]))

(defn sunset-items-by-age [ list-id age-limit ]
  (let [ toto-user-id (data/get-system-user-id)]
    (doseq [ item (data/get-sunset-items-by-id list-id age-limit)]
      (data/delete-item-by-id toto-user-id (:item_id item)))))

(defn item-sunset-job []
  (doseq [list-info (data/get-todo-lists-with-item-age-limit)]
    (sunset-items-by-age (:todo_list_id list-info)
                         (:max_item_age list-info))))
