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

(ns toto.todo.todo-list-manager
  (:use playbook.core
        compojure.core
        toto.core.view.common
        toto.core.view.icons
        toto.core.view.components
        toto.core.view.query
        toto.core.view.page
        toto.todo.ids)
  (:require [taoensso.timbre :as log]
            [hiccup.form :as hiccup-form]
            [hiccup.util :as hiccup-util]
            [toto.data.data :as data]
            [toto.core.view.auth :as auth]
            [toto.todo.sidebar :as sidebar]))

(defn- list-priority-button [ list-id new-priority image-spec ]
  (post-button {:target (str "/list/" (encode-list-id list-id) "/priority")
                :args {:new-priority new-priority}
                :desc "Set List Priority"}
               image-spec))

(defn render-list-star-control [ list-id priority ]
  (if (<= priority 0)
    (list-priority-button list-id 1 img-star-gray)
    (list-priority-button list-id 0 img-star-yellow)))

(defn render-new-list-form [ ]
  (hiccup-form/form-to
   {:class "new-item-form"}
   [:post (shref "/list")]
   (hiccup-form/text-field {:maxlength "32"
                            :placeholder "New List Name"
                            :autofocus "autofocus"}
                           "list-description")
   [:select {:id "list-type" :name "list-type"}
    (hiccup-form/select-options [["List" "list"]
                                 ["View" "view"]])]))

(defn- render-list-manager-query-settings []
  [:div.query-settings
   [:div.control-segment
    [:a {:href (shref "/lists" {:deleted "yes"})}
     "[include deleted]" ]]
   [:div.control-segment
    [:a {:href (str "/lists")}
     " [default view]"]]])

(defn- render-list-manager-entry [ list-info ]
  (let [list-id (:todo_list_id list-info)
        priority (:priority list-info)
        desc (:desc list-info)
        item-count (:item_count list-info)
        is-deleted? (:is_deleted list-info)]
    [:div.item-row {:class (class-set {"high-priority" (> priority 0)
                                       "low-priority" (< priority 0)
                                       "deleted-item" is-deleted?})}
     [:div.item-control
      (render-list-star-control list-id priority)]
     [:div.item-description
      [:a.list-link {:href (shref "/list/" (encode-list-id list-id) without-modal)}
       (hiccup.util/escape-html desc)
       [:span.pill item-count]]
      (if (< priority 0)
        (list-priority-button list-id 0 "Unhide List")
        (list-priority-button list-id -1 "Hide List"))
            (sidebar/render-list-visibility-flag list-info)
]

     [:a.details-link {:href (shref "/list/" (encode-list-id list-id)
                                    {:modal "details"})}
      "Details"]
     [:a.details-link {:href (shref "/list/" (encode-list-id list-id) "/completions")}
      "Completed"]
     [:a.details-link {:href (shref "/list/" (encode-list-id list-id)
                                    {:modal "share-with"})}
      "Sharing"]]))




(defn render-list-manager-page [ params ]
  (let [include-deleted (= (:deleted params) "yes")]
    (render-page
     {:title "Manage Todo Lists"}
     (scroll-column
      "todo-list-list-scroller"
      (render-new-list-form)
      [:div.toplevel-list.list-list
       (map render-list-manager-entry
            (data/get-todo-lists-by-user (auth/current-user-id) include-deleted))]
      (render-list-manager-query-settings)))))
