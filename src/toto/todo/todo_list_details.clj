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
            [toto.todo.data.data :as data]
            [toto.view.auth :as auth]
            [toto.todo.sidebar :as sidebar]))

(defn- render-todo-list-view-editor [ view-id ]
  (let [user-id (auth/current-user-id)
        todo-lists (data/get-todo-lists-by-user user-id false)
        view-sublist-ids (map :sublist_id (data/get-view-sublists user-id view-id))]
    [:div.config-panel
     [:h1 "Component Lists"]
     (scroll-column
      "component-lists"
      nil
      (map (fn [ todo-list ]
             (let [ list-id (:todo_list_id todo-list) ]
               [:div
                (hiccup-form/check-box (str "list_" list-id)
                                       (in? view-sublist-ids list-id))
                (hiccup-util/escape-html
                 (:desc todo-list))]))
           (remove #(:is_view %) todo-lists)))]))

(defn- render-item-sunset-panel [ max-item-age ]
  [:div.config-panel
   [:h1 "Item Age Limit"]
   [:div
    [:p
     "The maximum age for an item on this list. If this is set, items older than "
     "this number of days will automatically be deleted."]
    (render-duration-select "max-item-age" max-item-age [7 14 30 90] false)]])

(defn render-todo-list-details-modal [ list-id ]
  (let [list-details (data/get-todo-list-by-id list-id)
        list-name (:desc list-details)
        is-view (:is_view list-details)
        list-type (if is-view "View" "List")
        max-item-age (:max_item_age list-details)]
    (render-modal
     {:title (str "List Details: " (hiccup-util/escape-html list-name))
      :form-post-to (shref "/list/" (encode-list-id list-id) "/details")}
     [:div.config-panel
      [:h1 (str list-type " Name:")]
      (hiccup-form/text-field { :maxlength "32" } "list-name" list-name)]
     (if is-view
       (render-todo-list-view-editor list-id)
       (render-item-sunset-panel max-item-age))
     [:div.config-panel
      [:h1 "Delete List"]
      "This list may be deleted and archived "
      [:a {:href (shref {:modal "delete-list"})} "here"] "."]
     [:div.modal-controls
       [:input {:type "submit" :value "Update List Details"}]])))

(defn- render-list-delete-panel [ list-id ]
  [:div.config-panel
   [:h1 "Delete List"]
   (if (<= (data/get-user-list-count (auth/current-user-id)) 1)
     [:span.warning "Your last list cannot be deleted."]
     [:form {:method "POST"}
      (if (data/empty-list? list-id)
        [:span
         "Confirm list delete"]
        [:span.warning
         "This list still has active items. If you delete the list, they "
         "will no longer be visible."])
      [:div.modal-controls
       [:input.dangerous
        {:type "submit" :value "Delete List"
         :formaction (shref "/list/" (encode-list-id list-id) "/delete")}]]])])
