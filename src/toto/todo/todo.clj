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

(defn redirect-to-home []
  (redirect-to-list (current-todo-list-id)))

(defn redirect-to-lists []
  (ring/redirect "/lists"))

(defn update-list-description [ list-id list-description ]
  (when (not (string-empty? list-description))
    (data/update-list-description list-id list-description ))
  (ring/redirect (shref "/list/" list-id)))

(defn delete-list [ list-id ]
  (if (<= (data/get-user-list-count (auth/current-user-id)) 1)
    (log/warn "Attempt to delete user's last visible list" list-id)
    (data/delete-list list-id))
  (redirect-to-home))

(defn sort-list [ list-id sort-by ]
  (case sort-by
    "desc" (data/order-list-items-by-description! list-id)
    "created-on" (data/order-list-items-by-updated-on! list-id)
    "updated-on" (data/order-list-items-by-created-on! list-id)
    "snoozed-until" (data/order-list-items-by-snoozed-until! list-id))
  (redirect-to-list list-id))

(defn copy-list [list-id copy-from-list-id]
  (data/copy-list (auth/current-user-id) list-id copy-from-list-id)
  (redirect-to-list list-id))

(defn add-list [ list-description ]
  (if (string-empty? list-description)
    (redirect-to-home)
    (let [ list-id (data/add-list list-description) ]
      (data/set-list-ownership list-id #{ (auth/current-user-id) })
      (redirect-to-lists))))

(defn selected-user-ids-from-params [ params ]
  (set
   (map #(Integer/parseInt (.substring % 5))
        (filter #(.startsWith % "user_") (map name (keys params))))))

(defn update-list-details [ list-id params list-name share-with-email is-public ]
  (catch-validation-errors
   (let [share-with-email-id
         (when-let [ share-with-email (parsable-string? share-with-email) ]
           (or
            (auth/get-user-id-by-email share-with-email)
            (fail-validation
             (todo-list-manager/render-todo-list-details-page list-id
                                                              (or (parsable-integer? (:min-list-priority params)) 0)
                                                              :error-message "Invalid e-mail address"))))
         selected-ids (selected-user-ids-from-params params)]
     (data/set-list-ownership list-id
                              (if share-with-email-id
                                (conj selected-ids share-with-email-id)
                                selected-ids))
     (update-list-description list-id (string-leftmost list-name 32))
     (data/set-list-public list-id (boolean is-public))
     (ring/redirect  (shref "/list/" list-id "/details")))))

(defn update-list-priority [ list-id user-id new-priority ]
  (data/set-list-priority list-id user-id new-priority)
  (redirect-to-lists))

(defn add-item [ list-id item-description item-priority ]
  (when (not (string-empty? item-description))
    (data/add-todo-item (auth/current-user-id) list-id item-description item-priority))
  (redirect-to-list list-id))

(defn success []
  (ring/response "ok"))

(defn update-item-order [ item-id new-ordinal new-priority ]
  (let [list-id (data/get-list-id-by-item-id item-id)]
    (data/shift-list-items! list-id new-ordinal)
    (data/update-item-ordinal! item-id new-ordinal)
    (data/update-item-priority-by-id (auth/current-user-id) item-id new-priority))
  (success))

(defn update-item-desc [ item-id item-description ]
  (let [ list-id (data/get-list-id-by-item-id item-id)]
    (when (not (string-empty? item-description))
      (data/update-item-desc-by-id (auth/current-user-id) item-id item-description))
    (success)))

(defn update-item-snooze-days [ item-id snooze-days ]
  (let [list-id (data/get-list-id-by-item-id item-id)
        snooze-days (or (parsable-integer? snooze-days) 0)]
    (data/update-item-snooze-by-id (auth/current-user-id) item-id
                                   (if (= snooze-days 0)
                                     nil
                                     (add-days (java.util.Date.) snooze-days)))
    (success)))

(defn update-item-list [ item-id target-list-id ]
  (let [ original-list-id (data/get-list-id-by-item-id item-id)]
    (data/update-item-list (auth/current-user-id) item-id target-list-id)
    (success)))

(defn update-item-priority [ item-id new-priority ]
  (let [ original-list-id (data/get-list-id-by-item-id item-id)]
    (data/update-item-priority-by-id (auth/current-user-id) item-id new-priority)
    (success)))

(defn complete-item [ item-id ]
  (let [ list-id (data/get-list-id-by-item-id item-id)]
    (data/complete-item-by-id (auth/current-user-id) item-id)
    (success)))

(defn delete-item [ item-id ]
  (let [ list-id (data/get-list-id-by-item-id item-id)]
    (data/delete-item-by-id (auth/current-user-id) item-id)
    (redirect-to-list list-id)))

(defn restore-item [ item-id ]
  (let [ list-id (data/get-list-id-by-item-id item-id)]
    (data/restore-item (auth/current-user-id) item-id)
    (success)))

(defn- public-routes [ config ]
  (routes
   (GET "/list/:list-id/public" { { list-id :list-id } :params }
     ;; Retain backward compatibility with older public list URL scheme
     (redirect-to-list list-id))

   (GET "/list/:list-id" { { list-id :list-id } :params }
     (log/debug "public render: " list-id)
     (when (and (data/list-public? list-id)
                (not (data/list-owned-by-user-id? list-id (auth/current-user-id))))
       (todo-list/render-todo-list-public-page list-id)))

   (GET "/stocking/:list-id" { { list-id :list-id } :params }
     (todo-list/render-stocking-page list-id nil))

   (GET "/stocking/:list-id/:item-id" { { list-id :list-id item-id :item-id } :params }
     (todo-list/render-stocking-page list-id (parsable-integer? item-id)))))

(defn- list-routes [ list-id ]
  (ensure-list-owner-access list-id)
  (routes
   (GET "/" { params :params }
     (todo-list/render-todo-list-page list-id
                                      (parsable-integer? (:edit-item-id params))
                                      (or (parsable-integer? (:min-list-priority params)) 0)
                                      (or (parsable-integer? (:cwithin params)) 0)
                                      (or (parsable-integer? (:sfor params)) 0)
                                      (parsable-integer? (:snoozing params))
                                      (= (:updating-from params) "Y")))

   (GET "/list.csv" []
     (-> (todo-list/render-todo-list-csv list-id)
         (ring-response/response)
         (ring-response/header "Content-Type" "text/csv")))

   (GET "/details" { params :params }
     (todo-list-manager/render-todo-list-details-page list-id (or (parsable-integer? (:min-list-priority params)) 0)))

   (POST "/details" { params :params }
     (update-list-details list-id
                          params
                          (:list-name params) (:share-with-email params) (:is_public params)))

   (POST "/priority" { { new-priority :new-priority } :params }
     (update-list-priority list-id (auth/current-user-id) new-priority))

   (POST "/delete" []
     (delete-list list-id))

   (POST "/sort" { { sort-by :sort-by } :params }
     (sort-list list-id sort-by))

   (POST "/copy-from" { { copy-from-list-id :copy-from-list-id } :params }
     (ensure-list-owner-access copy-from-list-id)
     (copy-list list-id copy-from-list-id))

   (POST "/" { { item-description :item-description item-priority :item-priority } :params }
     (add-item list-id (string-leftmost item-description 1024) item-priority))))

(defn- item-routes [ item-id ]
  (ensure-item-access item-id)
  (routes
   (POST "/"  { params :params }
     (update-item-desc item-id (string-leftmost (:description params) 1024)))

   (POST "/snooze" { params :params }
     (update-item-snooze-days item-id (:snooze-days params)))

   (POST "/priority" { params :params }
     (update-item-priority item-id (:new-priority params)))

   (POST "/complete" [ ]
     (complete-item item-id))

   (POST "/delete" [ ]
     (delete-item item-id))

   (POST "/restore" [ ]
     (restore-item item-id))))

(defn- private-routes [ config ]
  (routes
   (GET "/" [] (redirect-to-home))

   (POST "/list" { params :params }
     (add-list (string-leftmost (:list-description params) 32)))

   (GET "/lists" []
     (todo-list-manager/render-list-list-page))

   (context "/list/:list-id" [ list-id ]
     (list-routes list-id))

   (POST "/item-list" { { target-item :target-item target-list :target-list} :params}
     (ensure-item-access target-item)
     (ensure-list-owner-access target-list)
     (update-item-list target-item target-list))

   (POST "/item-order" {{target-item :target-item
                         new-ordinal :new-ordinal
                         new-priority :new-priority} :params
                        headers :headers }
     (ensure-item-access target-item)
     (update-item-order target-item new-ordinal new-priority))

   (context "/item/:item-id" [ item-id ]
     (item-routes item-id))))

(defn all-routes [ config ]
  (routes
   (public-routes config)
   (auth/authorize-toto-valid-user (private-routes config))))
