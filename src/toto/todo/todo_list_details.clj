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
;; You must not remove this notice, or any other, from this

(ns toto.todo.todo-list-details
  (:use playbook.core
        compojure.core
        toto.view.common
        toto.view.icons
        toto.view.components
        toto.view.query
        toto.view.page
        toto.todo.ids)
  (:require [taoensso.timbre :as log]
            [hiccup.form :as hiccup-form]
            [hiccup.util :as hiccup-util]
            [toto.data.data :as data]
            [toto.view.auth :as auth]
            [toto.todo.sidebar :as sidebar]))

(defn- render-sort-list-panel [ list-id ]
  [:div.config-panel
   [:h1 "Sort List"]
   (hiccup-form/form-to {} [:post (shref "/list/" (encode-list-id list-id) "/sort")]
    [:input {:type "submit" :value "Sort By"}]
    [:select {:id "sort-by" :name "sort-by"}
     (hiccup-form/select-options [["Description" "desc"]
                                  ["Created Date" "created-on"]
                                  ["Updated Date" "updated-on"]
                                  ["Snoozed Until" "snoozed-until"]])])])

(defn- render-list-delete-panel [ list-id ]
  [:div.config-panel
   [:h1 "Delete List"]
   (cond
     (<= (data/get-user-list-count (auth/current-user-id)) 1)
     [:span.warning "Your last list cannot be deleted."]

     (not (data/empty-list? list-id))
     [:span.warning "To delete this list, remove all items first."]

     :else
     (list
      [:form {:method "POST"}
       [:input.dangerous {:type "submit" :value "Delete List"
                          :formaction (shref "/list/" (encode-list-id list-id) "/delete")}]
       [:span.warning "Warning, this cannot be undone."]]))])

(defn- render-todo-list-view-editor [ view-id ]
  (let [user-id (auth/current-user-id)
        todo-lists (data/get-todo-lists-by-user user-id)
        view-sublist-ids (map :sublist_id (data/get-view-sublists user-id view-id))]
    [:div.config-panel
     [:h1 "Component Lists"]
     [:div.component-lists
      (map (fn [ todo-list ]
             (let [ list-id (:todo_list_id todo-list) ]
               [:div
                (hiccup-form/check-box (str "list_" list-id)
                                       (in? view-sublist-ids list-id))
                [:a {:href (shref "/list/" (encode-list-id list-id) "/details")}
                 (hiccup-util/escape-html
                  (:desc todo-list))]]))
           (remove #(:is_view %) todo-lists))]]))

(defn- render-item-sunset-panel [ max-item-age ]
  [:div.config-panel
   [:h1 "Item Age Limit"]
   [:div
    [:p
     "The maximum age for an item on this list. If this is set, items older than "
     "this number of days will automatically be deleted."]
    (render-duration-select "max-item-age" max-item-age [7 14 30 90] false)]])

(defn render-todo-list-details-page [ list-id min-list-priority & { :keys [ error-message ]}]
  (let [list-details (data/get-todo-list-by-id list-id)
        list-name (:desc list-details)
        is-view (:is_view list-details)
        list-type (if is-view "View" "List")
        max-item-age (:max_item_age list-details)]
    (render-page
     {:title (str list-type " Details: " (hiccup-util/escape-html list-name))
      :sidebar (sidebar/render-sidebar list-id min-list-priority 0)}
     (scroll-column
      "todo-list-details-column"
      [:h3
       [:a { :href (str "/list/" list-id ) } img-back-arrow]
       "List Details: " (hiccup-util/escape-html list-name)]
      (hiccup-form/form-to
       {:class "details"}
       [:post (shref "/list/" (encode-list-id list-id) "/details")]
       [:div.config-panel
        [:h1 (str list-type " Name:")]
        (hiccup-form/text-field { :maxlength "32" } "list-name" list-name)]
       (if is-view
         (render-todo-list-view-editor list-id)
         (render-item-sunset-panel max-item-age))
       [:div.config-panel
        [:div
         [:input {:type "submit" :value "Update List Details"}]]])
      (when (not is-view)
        (render-sort-list-panel list-id))
      (render-list-delete-panel list-id)))))
