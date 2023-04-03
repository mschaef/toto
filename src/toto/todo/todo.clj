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

(ns toto.todo.todo
  (:use toto.core.util
        compojure.core
        toto.view.common
        toto.view.query
        toto.view.page)
  (:require [clojure.tools.logging :as log]
            [ring.util.response :as ring]
            [compojure.handler :as handler]
            [ring.util.response :as ring-response]
            [cemerick.friend :as friend]
            [toto.data.data :as data]
            [toto.view.auth :as auth]
            [toto.todo.landing-page :as landing-page]
            [toto.todo.todo-list :as todo-list]
            [toto.todo.todo-list-manager :as todo-list-manager]))

(defn current-todo-list-id []
  (auth/authorize-expected-roles
   (first (data/get-todo-list-ids-by-user (auth/current-user-id)))))

(defn ensure-list-owner-access [ list-id ]
  (auth/authorize-expected-roles
   (unless (data/list-owned-by-user-id? list-id (auth/current-user-id))
           (auth/report-unauthorized))))

(defn ensure-item-access [ item-id ]
  (auth/authorize-expected-roles
   (unless (data/item-owned-by-user-id? item-id (auth/current-user-id))
           (auth/report-unauthorized))))

(defn redirect-to-list [ list-id ]
  (ring/redirect (shref "/list/" list-id)))

(defn redirect-to-home-list []
  (redirect-to-list (current-todo-list-id)))

(defn redirect-to-lists []
  (ring/redirect "/lists"))

(defn success []
  (ring/response "ok"))

(defn- update-list-description [ list-id list-description ]
  (when (not (string-empty? list-description))
    (data/update-list-description list-id list-description ))
  (ring/redirect (shref "/list/" list-id)))

(defn- delete-list [ list-id ]
  (if (<= (data/get-user-list-count (auth/current-user-id)) 1)
    (log/warn "Attempt to delete user's last visible list" list-id)
    (data/delete-list list-id))
  (redirect-to-home-list))

(defn- sort-list [ list-id params ]
  (let [{ sort-by :sort-by } params]
    (case sort-by
      "desc" (data/order-list-items-by-description! list-id)
      "created-on" (data/order-list-items-by-updated-on! list-id)
      "updated-on" (data/order-list-items-by-created-on! list-id)
      "snoozed-until" (data/order-list-items-by-snoozed-until! list-id))
    (redirect-to-list list-id)))

(defn- copy-list [ list-id params ]
  (let [ { copy-from-list-id :copy-from-list-id } params ]
    (ensure-list-owner-access copy-from-list-id)
    (data/copy-list (auth/current-user-id) list-id copy-from-list-id)
    (redirect-to-list list-id)))

(defn- add-list [ params ]
  (let [{list-description :list-description
         is-view :is-view} params
        list-description (string-leftmost list-description 32)
        is-view (= is-view "Y")]
    (if (string-empty? list-description)
      (redirect-to-home-list)
      (let [ list-id (data/add-list list-description is-view) ]
        (data/set-list-ownership list-id #{ (auth/current-user-id) })
        (redirect-to-lists)))))

(defn- selected-user-ids-from-params [ params ]
  (set
   (map #(Integer/parseInt (.substring % 5))
        (filter #(.startsWith % "user_") (map name (keys params))))))

(defn- selected-sublist-ids-from-params [ params ]
  (set
   (map #(Integer/parseInt (.substring % 5))
        (filter #(.startsWith % "list_") (map name (keys params))))))

(defn- update-view-details [ list-id params ]
  (let [user-id (auth/current-user-id)
        possible-sublist-ids (data/get-todo-list-ids-by-user user-id)
        selected-sublist-ids (filter #(in? possible-sublist-ids %)
                                     (selected-sublist-ids-from-params params))]
    (data/set-view-sublist-ids user-id list-id selected-sublist-ids)
    (update-list-description list-id (string-leftmost (:list-name params) 32))
    (ring/redirect  (shref "/list/" list-id "/details")))  )

(defn- update-list-details [ list-id params ]
  (let [share-with-email (parsable-string? (:share-with-email params))
        share-with-email-id (and share-with-email
                                 (auth/get-user-id-by-email share-with-email))
        selected-user-ids (selected-user-ids-from-params params)]
    (if (and share-with-email
             (not share-with-email-id))
      (unprocessable-entity
       (todo-list-manager/render-todo-list-details-page list-id
                                                        (or (parsable-integer? (:min-list-priority params)) 0)
                                                        :error-message "Invalid e-mail address"))
      (do
        (data/set-list-ownership list-id
                                 (if share-with-email-id
                                   (conj selected-user-ids share-with-email-id)
                                   selected-user-ids))
        (update-list-description list-id (string-leftmost (:list-name params) 32))
        (data/set-list-public list-id (boolean (:is-public params)))
        (ring/redirect  (shref "/list/" list-id "/details"))))))

(defn- update-list-or-view-details [ list-id params ]
  (if (:is_view (data/get-todo-list-by-id list-id))
    (update-view-details list-id params)
    (update-list-details list-id params)))

(defn- update-list-priority [ list-id new-priority ]
  (data/set-list-priority list-id (auth/current-user-id) new-priority)
  (redirect-to-lists))

(defn- add-item [ list-id params ]
  (let [{item-list-id :item-list-id
         item-description :item-description
         item-priority :item-priority } params
        item-description (string-leftmost item-description 1024)]
    (ensure-list-owner-access item-list-id)
    (when (not (string-empty? item-description))
      (data/add-todo-item (auth/current-user-id) item-list-id item-description item-priority))
    (redirect-to-list list-id)))

(defn- update-item-ordinal [ item-id params ]
  (let [{target-list-id :target-list
         new-ordinal :new-ordinal
         new-priority :new-priority} params]
    (ensure-list-owner-access target-list-id)
    (data/update-item-list (auth/current-user-id) item-id target-list-id)
    (data/shift-list-items! target-list-id new-ordinal)
    (data/update-item-ordinal! item-id new-ordinal)
    (data/update-item-priority-by-id (auth/current-user-id) item-id new-priority))
  (success))

(defn- update-item-desc [ item-id params ]
  (let [{description :description} params
        description (string-leftmost description 1024)]
    (when (not (string-empty? description))
      (data/update-item-desc-by-id (auth/current-user-id) item-id description))
    (success)))

(defn- update-item-snooze-days [ item-id params ]
  (let [snooze-days (or (parsable-integer? (:snooze-days params)) 0)
        user-id (auth/current-user-id)]
    (if (= snooze-days 0)
      (data/remove-item-snooze-by-id user-id item-id)
      (data/update-item-snooze-by-id user-id item-id (add-days (current-time) snooze-days 5)))
    (success)))

(defn- update-item-list [ item-id params ]
  (let [ { target-list :target-list } params ]
    (ensure-list-owner-access target-list)
    (data/update-item-list (auth/current-user-id) item-id target-list)
    (success)))

(defn update-item-priority [ item-id params ]
  (let [ { new-priority :new-priority } params]
    (data/update-item-priority-by-id (auth/current-user-id) item-id new-priority)
    (success)))

(defn complete-item [ item-id ]
  (data/complete-item-by-id (auth/current-user-id) item-id)
  (success))

(defn delete-item [ item-id ]
  (let [ list-id (data/get-list-id-by-item-id item-id)]
    (data/delete-item-by-id (auth/current-user-id) item-id)
    (redirect-to-list list-id)))

(defn restore-item [ item-id ]
  (data/restore-item (auth/current-user-id) item-id)
  (success))

(defn- render-launch-page [ params ]
  (if (auth/current-identity)
    (redirect-to-home-list)
    (landing-page/render-landing-page params)))

(defn- public-routes [ config ]
  (routes
   (GET "/" { params :params }
     (render-launch-page params))

   (GET "/list/:list-id/public" { { list-id :list-id } :params }
     ;; Retain backward compatibility with older public list URL scheme
     (redirect-to-list list-id))

   (GET "/list/:list-id" { params :params }
     (todo-list/render-todo-list-public-page params))))

(defn- list-routes [ list-id ]
  (ensure-list-owner-access list-id)
  (routes
   (GET "/" { params :params }
     (todo-list/render-todo-list-page list-id params))

   (GET "/completions" { params :params }
     (todo-list/render-todo-list-completions-page list-id params))

   (POST "/" { params :params }
     (add-item list-id params))

   (GET "/details" { params :params }
     (todo-list-manager/render-todo-list-details-page
      list-id (or (parsable-integer? (:min-list-priority params)) 0)))

   (POST "/details" { params :params }
     (update-list-or-view-details list-id params))

   (POST "/priority" { { new-priority :new-priority } :params }
     (update-list-priority list-id new-priority))

   (POST "/delete" []
     (delete-list list-id))

   (POST "/sort" { params :params }
     (sort-list list-id params))

   (POST "/copy-from" { params :params }
     (copy-list list-id params))))

(defn- item-routes [ item-id ]
  (ensure-item-access item-id)
  (routes
   (POST "/"  { params :params }
     (update-item-desc item-id params))

   (POST "/snooze" { params :params }
     (update-item-snooze-days item-id params))

   (POST "/priority" { params :params }
     (update-item-priority item-id params))

   (POST "/complete" [ ]
     (complete-item item-id))

   (POST "/delete" [ ]
     (delete-item item-id))

   (POST "/restore" [ ]
     (restore-item item-id))

   (POST "/list" { params :params }
     (update-item-list item-id params))

   (POST "/ordinal" { params :params }
     (update-item-ordinal item-id params))))

(defn- private-routes [ config ]
  (routes
   (POST "/list" { params :params }
     (add-list params))

   (GET "/lists" []
     (todo-list-manager/render-list-list-page))

   (context "/list/:list-id" [ list-id ]
     (list-routes list-id))

   (context "/item/:item-id" [ item-id ]
     (item-routes item-id))))

(defn all-routes [ config ]
  (routes
   (public-routes config)
   (auth/authorize-toto-valid-user (private-routes config))))
