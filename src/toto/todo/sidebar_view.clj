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

(ns toto.todo.sidebar-view
  (:use toto.core.util
        toto.view.common
        toto.view.icons
        toto.view.query)
  (:require [clojure.tools.logging :as log]
            [toto.data.data :as data]
            [toto.view.auth :as auth]))

(defn render-list-visibility-flag [ list ]
  (let [{is-public :is_public
         list-owner-count :list_owner_count
         is-view :is_view}
        list]
    (cond
      is-view
      ;; Views cannot be shared, so the other flags do not apply.
      [:span.list-visibility-flag img-folders]

      is-public
      [:span.list-visibility-flag img-globe]

      (> list-owner-count 1)
      [:span.list-visibility-flag img-group])))

(defn render-sidebar-list-list [ selected-list-id min-list-priority snoozed-for-days ]
  (let [include-low-priority (< min-list-priority 0)]
    [:div.list-list
     (map (fn [ list ]
            (let [{list-id :todo_list_id
                   list-desc :desc
                   list-item-count :item_count
                   list-total-item-count :total_item_count
                   is-public :is_public
                   is-view :is_view
                   list-owner-count :list_owner_count
                   priority :priority}
                  list ]
              [:div.list-row {:class (class-set {"selected" (= list-id (Integer. selected-list-id))
                                                 "view" is-view
                                                 "high-priority" (and include-low-priority (> priority 0))
                                                 "low-priority" (and include-low-priority (< priority 0))})
                              :listid list-id}
               [:a.item {:href (shref "/list/" list-id)}
                (hiccup.util/escape-html list-desc)
                (render-list-visibility-flag list)]
               (if (not is-view)
                 [:span.pill {:class (class-set {"highlight" (and (> snoozed-for-days 0)
                                                                  (not (= list-item-count list-total-item-count)))
                                                 "emphasize" (not (= list-item-count list-total-item-count))})}
                  (if (> snoozed-for-days 0)
                    list-total-item-count
                    list-item-count)])]))
          (remove #(and (< (:priority %) min-list-priority)
                        (not (= (Integer. selected-list-id) (:todo_list_id %))))
                  (data/get-todo-lists-by-user (auth/current-user-id))))

     [:div.control-row
      (if (< min-list-priority 0)
        [:a {:href (shref "" {:min-list-priority 0})} "Hide Hidden Lists"]
        [:a {:href (shref "" {:min-list-priority -1})} "Show All Lists"])]

     [:div.control-row
      [:a {:href "/lists"} "Manage Todo Lists"]]]))
