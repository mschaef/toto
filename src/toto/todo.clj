(ns toto.todo
  (:use toto.util
        compojure.core
        toto.view-utils)
  (:require [clojure.tools.logging :as log]
            [ring.util.response :as ring]
            [compojure.handler :as handler]
            [ring.util.response :as ring-response]
            [cemerick.friend :as friend]
            [toto.data :as data]
            [toto.list-view :as list-view]
            [toto.list-manager-view :as list-manager-view]
            [toto.user :as user]))

(defn current-todo-list-id []
  (friend/authorize user/expected-roles
                    (first (data/get-todo-list-ids-by-user (current-user-id)))))

(defn ensure-list-owner-access [ list-id ]
  (friend/authorize user/expected-roles
                    (unless (data/list-owned-by-user-id? list-id (current-user-id))
                            (report-unauthorized))))

(defn ensure-list-public-access [ list-id ]
  (unless (data/list-public? list-id)
          (report-unauthorized)))

(defn ensure-item-access [ item-id ]
  (friend/authorize user/expected-roles
                    (unless (data/item-owned-by-user-id? item-id (current-user-id))
                            (report-unauthorized))))

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
  (data/delete-list list-id)
  (redirect-to-home))

(defn sort-list [ list-id ]
  (data/order-list-items-by-description! list-id)
  (redirect-to-list list-id))

(defn get-user-id-by-email [ email ]
  (if-let [ user-info (data/get-user-by-email email) ]
    (user-info :user_id)
    nil))

(defn add-list [ list-description ]
  (if (string-empty? list-description)
    (redirect-to-home)
    (let [ list-id (data/add-list list-description) ]
      (data/set-list-ownership list-id #{ (current-user-id) })
      (redirect-to-lists))))

(defn selected-user-ids-from-params [ params ]
  (set
   (map #(Integer/parseInt (.substring % 5))
        (filter #(.startsWith % "user_") (map name (keys params))))))

(defn update-list-details [ list-id params list-name share-with-email is-public ]
  (catch-validation-errors
   (let [share-with-email-id
         (and share-with-email
              (or
               (get-user-id-by-email share-with-email)
               (fail-validation
                (list-manager-view/render-todo-list-details-page list-id
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
    (data/add-todo-item (current-user-id) list-id item-description item-priority))
  (redirect-to-list list-id))

(defn success []
  (ring/response "ok"))

(defn update-item-order [ item-id new-ordinal new-priority referrer ]
  (let [list-id (data/get-list-id-by-item-id item-id)]
    (data/shift-list-items! list-id new-ordinal)
    (data/update-item-ordinal! item-id new-ordinal)
    (data/update-item-priority-by-id (current-user-id) item-id new-priority))
  (success))

(defn update-item-desc [ item-id item-description referrer ]
  (let [ list-id (data/get-list-id-by-item-id item-id)]
    (when (not (string-empty? item-description))
      (data/update-item-desc-by-id (current-user-id) item-id item-description))
    (ring/redirect referrer)))

(defn update-item-snooze-days [ item-id snooze-days referrer ]
  (let [list-id (data/get-list-id-by-item-id item-id)
        snooze-days (or (parsable-integer? snooze-days) 0)]
    (data/update-item-snooze-by-id  (current-user-id) item-id
                                    (if (= snooze-days 0)
                                      nil
                                      (add-days (java.util.Date.) snooze-days)))
    (success)))

(defn update-item-list [ item-id target-list-id ]
  (let [ original-list-id (data/get-list-id-by-item-id item-id)]
    (data/update-item-list (current-user-id) item-id target-list-id)
    (success)))

(defn update-item-priority [ item-id new-priority referrer ]
  (log/info [ item-id new-priority referrer ])
  (let [ original-list-id (data/get-list-id-by-item-id item-id)]
    (data/update-item-priority-by-id (current-user-id) item-id new-priority)
    (success)))

(defn complete-item [ item-id referrer ]
  (let [ list-id (data/get-list-id-by-item-id item-id)]
    (data/complete-item-by-id (current-user-id) item-id)
    (success)))

(defn delete-item [ item-id referrer ]
  (let [ list-id (data/get-list-id-by-item-id item-id)]
    (data/delete-item-by-id (current-user-id) item-id)
    (redirect-to-list list-id)))

(defn restore-item [ item-id ]
  (let [ list-id (data/get-list-id-by-item-id item-id)]
    (data/restore-item (current-user-id) item-id)
    (success)))

(defn- public-routes [ config ]
  (routes
   (GET "/list/:list-id/public" { { list-id :list-id } :params }
     ;; Retain backward compatibility with older public list URL scheme
     (redirect-to-list list-id))

   (GET "/list/:list-id" { { list-id :list-id } :params }
     (log/debug "public render: " list-id)
     (when (and (data/list-public? list-id)
                (not (data/list-owned-by-user-id? list-id (current-user-id))))
       (list-view/render-todo-list-public-page list-id)))))

(defn- list-routes [ list-id ]
  (ensure-list-owner-access list-id)
  (routes
   (GET "/" { params :params }
     (list-view/render-todo-list-page list-id
                                      (or (parsable-integer? (:cwithin params)) 0)
                                      (and (:include-snoozed params)
                                           (= (:include-snoozed params) "Y"))))

   (GET "/list.csv" []
     (-> (list-view/render-todo-list-csv list-id)
         (ring-response/response)
         (ring-response/header "Content-Type" "text/csv")))

   (GET "/details"  []
     (list-manager-view/render-todo-list-details-page list-id))

   (POST "/details" { params :params }
     (update-list-details list-id params
                          (:list-name params) (:share-with-email params) (:is_public params)))

   (POST "/priority" { { new-priority :new-priority } :params }
     (update-list-priority list-id (current-user-id) new-priority))

   (POST "/delete" []
     (delete-list list-id))

   (POST "/sort" []
     (sort-list list-id))

   (POST "/" { { item-description :item-description item-priority :item-priority } :params }
     (add-item list-id (string-leftmost item-description 1024) item-priority))))

(defn- item-routes [ item-id ]
  (ensure-item-access item-id)
  (routes
   (POST "/"  { params :params headers :headers }
     (update-item-desc item-id (string-leftmost (:description params) 1024)
                       (get headers "referer")))

   (POST "/snooze" { params :params headers :headers }
     (update-item-snooze-days item-id (:snooze-days params)
                              (get headers "referer")))

   (POST "/priority" { params :params headers :headers }
     (update-item-priority item-id (:new-priority params)
                           (get headers "referer")))

   (POST "/complete" { headers :headers }
     (complete-item item-id (get headers "referer")))

   (POST "/delete" { headers :headers }
     (delete-item item-id (get headers "referer")))

   (POST "/restore" [ ]
     (restore-item item-id))))

(defn- private-routes [ config ]
  (routes
   (GET "/" [] (redirect-to-home))

   (POST "/list" { params :params }
     (add-list (string-leftmost (:list-description params) 32)))

   (GET "/lists" []
     (list-manager-view/render-list-list-page))

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
     (update-item-order target-item new-ordinal new-priority (get headers "referer")))

   (context "/item/:item-id" [ item-id ]
     (item-routes item-id))))

(defn all-routes [ config ]
  (routes
   (public-routes config)
   (-> (private-routes config)
       (wrap-routes friend/wrap-authorize #{:toto.role/verified})
       (wrap-routes friend/wrap-authorize #{:toto.role/current-password}))))
