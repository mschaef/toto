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

(ns toto.todo.util
  (:use playbook.core
        toto.view.query
        toto.view.page
        toto.todo.ids)
  (:require [taoensso.timbre :as log]
            [ring.util.response :as ring]
            [toto.todo.data.data :as data]
            [toto.view.auth :as auth]))

(defn current-todo-list-id []
  (auth/authorize-expected-roles
   (first (data/get-todo-list-ids-by-user (auth/current-user-id)))))

(defn accept-authorized-list-id [ list-id ]
  (aand (decode-list-id list-id)
        (auth/authorize-expected-roles
         (if (data/list-owned-by-user-id? it (auth/current-user-id))
           it
           (auth/report-unauthorized)))))

(defn accept-authorized-item-id [ item-id ]
  (aand (decode-item-id item-id)
        (auth/authorize-expected-roles
         (if (data/item-owned-by-user-id? it (auth/current-user-id))
           it
           (auth/report-unauthorized)))))

(defn redirect-to-list
  ([ list-id ]
   (redirect-to-list list-id {}))

  ([ list-id params ]
   (ring/redirect (shref "/list/" (encode-list-id list-id) params))))

(defn redirect-to-home-list []
  (redirect-to-list (current-todo-list-id) without-modal))

(defn redirect-to-lists []
  (ring/redirect "/lists"))

(defn success []
  (ring/response "ok"))
