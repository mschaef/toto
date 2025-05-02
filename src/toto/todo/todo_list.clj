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
;; Thecense is also includes at the root of the project in the file
;; LICENSE.
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.
;;
;; You must not remove this notice, or any other, from this software.

(ns toto.todo.todo-list
  (:use playbook.core
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
            [toto.todo.modals :as modals]
            [toto.todo.sidebar :as sidebar]
            [toto.todo.todo-item :as todo-item]
            [toto.todo.todo-list-details :as todo-list-details]))

(defn- render-new-item-form [list-id editing-item? last-item-list-id]
  (let [sublists (data/get-view-sublists (auth/current-user-id) list-id)]
    (hiccup-form/form-to
     {:class "new-item-form"}
     [:post (shref "/list/" (encode-list-id list-id))]
     (if (= (count sublists) 0)
       (hiccup-form/hidden-field "item-list-id" list-id)
       [:select {:id "item-list-id" :name "item-list-id"}
        (hiccup-form/select-options (map (fn [sublist]
                                           [(hiccup-util/escape-html (:desc sublist))
                                            (encode-list-id (:sublist_id sublist))])
                                         sublists)
                                    last-item-list-id)])
     (hiccup-form/text-field (cond-> {:maxlength "1024"
                                      :placeholder "New Item Description"
                                      :autocomplete "off"
                                      :onkeydown "window._toto.onNewItemInputKeydown(event)"}
                               (not editing-item?) (assoc "autofocus" "on"))
                             "item-description"))))

(def query-durations [1 7 30 90 365 730])

(defn- render-todo-list-query-settings [list-id completed-within-days snoozed-for-days]
  (let [is-view (:is_view (data/get-todo-list-by-id list-id))]
    [:div.query-settings
     (hiccup-form/form-to {:class "embedded"} [:get (shref "/list/" (encode-list-id list-id))]
                          [:div.control-segment
                           [:a {:href (str "/list/" (encode-list-id list-id))}
                            "[default view]"]]
                          [:div.control-segment
                           [:a {:href (shref "/list/" (encode-list-id list-id)
                                             {:min-item-priority 1})}
                            "[only starred]"]]
                          [:div.control-segment
                           [:label {:for "cwithin"}
                            "Completed within: "]
                           (render-duration-select "cwithin" completed-within-days query-durations true)]
                          [:div.control-segment
                           [:a {:href (shref "/list/" (encode-list-id list-id)
                                             {:modal "details"})}
                            "[list details]"]]
                          [:div.control-segment
                           [:a.details-link {:href (shref "/list/" (encode-list-id list-id) "/completions")}
                            "[completed items]"]]
                          (when (not is-view)
                            [:div.control-segment
                             [:a {:href (shref "/list/" (encode-list-id list-id)
                                               {:modal "share-with"})}
                              "[share list]"]])
                          (when (not is-view)
                            [:div.control-segment
                             [:a {:href (shref "/list/" (encode-list-id list-id)
                                               {:modal "sort-list"})}
                              "[sort list]"]])
                          [:div.control-segment
                           [:a {:href (shref "/list/" (encode-list-id list-id)
                                             {:modal "update-from"})}
                            "[copy from]"]]
                          [:div.control-segment
                           (copy-button (todo-list-link list-id) "Copy Link")])]))

(defn- render-todo-list-completion-query-settings [list-id completed-within-days]
  [:div.query-settings
   (hiccup-form/form-to {:class "embedded "} [:get (shref "/list/" (encode-list-id list-id) "/completions")]
                        [:div.control-segment
                         [:a {:href (shref "/list/" (encode-list-id list-id) "/details")}
                          "[list details]"]]
                        [:div.control-segment
                         [:label {:for "cwithin"}
                          "Completed within: "]
                         (render-duration-select "clwithin" completed-within-days query-durations true)])])

(defn- render-empty-list [n-snoozed-items]
  [:div.empty-list
   [:h1
    "Nothing to do right now!"]
   (when (<= n-snoozed-items 0)
     [:p
      "To get started, you can add new items in the box above."])])

(defn- render-snoozed-item-query-settings [n-snoozed-items snoozed-for-days]
  (when (> n-snoozed-items 0)
    (if (> snoozed-for-days 0)
      [:div.snoozed-item-query-settings
       "Including snoozed items. Click " [:a {:href (shref "" {:sfor :remove})} "here"] " to hide."]
      [:div.snoozed-item-query-settings
       n-snoozed-items " more item" (if (= n-snoozed-items 1) "" "s") " snoozed for later. "
       "Click " [:a {:href (shref "" {:sfor "99999"})} "here"] " to show."])))

(defn- todo-list-details [view-list-id list-id edit-item-id writable? completed-within-days snoozed-for-days min-item-priority]
  (let [max-item-age (or (:max_item_age (data/get-todo-list-by-id list-id))
                         Integer/MAX_VALUE)
        pending-items (data/get-pending-items list-id completed-within-days snoozed-for-days min-item-priority)
        n-snoozed-items (count (filter :currently_snoozed pending-items))
        display-items (remove :visibly_snoozed pending-items)
        edit-item-id (if edit-item-id (decode-item-id edit-item-id))]

    {:is-empty? (= (count display-items) 0)
     :n-snoozed-items n-snoozed-items

     :high-priority
     (map #(todo-item/render-todo-item view-list-id list-id % writable? (= edit-item-id (:item_id %)) max-item-age)
          (filter #(> (:priority %) 0) display-items))

     :normal-priority
     (map #(todo-item/render-todo-item view-list-id list-id % writable? (= edit-item-id (:item_id %)) max-item-age)
          (filter #(<= (:priority %) 0) display-items))}))

(defn- render-single-todo-list [view-list-id list-id edit-item-id writable? completed-within-days snoozed-for-days min-item-priority]
  (let [details (todo-list-details view-list-id list-id edit-item-id writable? completed-within-days snoozed-for-days min-item-priority)
        n-snoozed-items (:n-snoozed-items details)]
    (list
     [:div.toplevel-list
      (if (:is-empty? details)
        (render-empty-list n-snoozed-items)
        (concat
         (:high-priority details)
         (:normal-priority details)))]
     (when writable?
       (render-snoozed-item-query-settings n-snoozed-items snoozed-for-days)))))

(defn- render-empty-view [list-id]
  [:div.empty-list
   [:h1
    "Empty list view!"]
   [:p
    "This is a todo list view, where it is possible to see the contents of"
    " more than one list on a single page. To make this work, you need to "
    " add a few lists to the view, which can be done "
    [:a {:href (shref "/list/" (encode-list-id list-id) {:modal "details"})}
     "here"] "."]])

(defn- render-todo-list-view-section [sublist-details key other-key]
  (let [items (key sublist-details)
        section-count (count items)
        snoozed-item-count (:n-snoozed-items sublist-details)]
    (when (> section-count 0)
      [:div.list-view-section
       [:h2
        [:a
         {:href (shref "/list/" (encode-list-id (:sublist_id sublist-details)))}
         (hiccup-util/escape-html
          (:desc sublist-details))]
        (let [other-count (count (get sublist-details other-key []))]
          [:span.other-item-count
           " ("
           (str section-count " starred")
           (when (> other-count 0)
             (str ", " other-count " other" (when (not= other-count 1) "s")))
           (when (> snoozed-item-count 0)
             (str ", " snoozed-item-count " snoozed"))
           ")"])]
       items])))

(defn- render-todo-list-view [list-id edit-item-id writable? completed-within-days snoozed-for-days min-item-priority]
  (let [sublists (map #(merge % (todo-list-details list-id (:sublist_id %) edit-item-id writable? completed-within-days snoozed-for-days min-item-priority))
                      (data/get-view-sublists (auth/current-user-id) list-id))
        total-snoozed-items (apply + (map :n-snoozed-items sublists))]
    [:div.toplevel-list
     (cond
       (= 0 (count sublists))
       (render-empty-view list-id)

       (every? true? (map :is-empty? sublists))
       (render-empty-list total-snoozed-items)

       :else
       (list
        (map #(render-todo-list-view-section % :high-priority :normal-priority) sublists)
        (map #(render-todo-list-view-section % :normal-priority false) sublists)
        (when writable?
          (render-snoozed-item-query-settings total-snoozed-items snoozed-for-days))))]))

(defn- message-recepient? []
  (or (= 16 (auth/current-user-id))
      (= 17 (auth/current-user-id))))

(defn- render-valentines-day-banner []
  (when (message-recepient?)
    [:div.banner.valentines-day
     img-heart-red
     (if (todo-item/valentines-day?)
       " Happy Valentines Day!!! I Love You! "
       " I Love You, Teresa! ")
     img-heart-red]))

(defn- render-sunset-banner [list-id]
  (when-let [max-item-age (:max_item_age (data/get-todo-list-by-id list-id))]
    [:div.banner.sunset
     "This list automatically sunsets items older than " max-item-age
     " day" (if (not (= max-item-age 1)) "s") "."]))

(defn- render-todo-list [list-id edit-item-id writable? completed-within-days snoozed-for-days last-item-list-id min-item-priority]
  (scroll-column
   (str "todo-list-scroller-" list-id)
   (when writable?
     (render-new-item-form list-id (boolean edit-item-id) last-item-list-id))
   (render-sunset-banner list-id)
   (list
    (render-valentines-day-banner)
    (if (:is_view (data/get-todo-list-by-id list-id))
      (render-todo-list-view list-id edit-item-id writable? completed-within-days snoozed-for-days min-item-priority)
      (render-single-todo-list list-id list-id edit-item-id writable? completed-within-days snoozed-for-days min-item-priority))
    (when writable?
      (render-todo-list-query-settings list-id completed-within-days snoozed-for-days)))))

(defn- render-empty-completion-list [list-id]
  (let [n-items (data/get-item-count list-id)]
    [:div.empty-list
     [:h1
      "Nothing here right now!"]
     [:p
      (if (= n-items 0)
        "As you add items and complete them, they will appear here."
        "Try expanding the query to see earlier completed items.")]]))

(defn- completed-items [list-id completed-within-days]
  (let [list-info (data/get-todo-list-by-id list-id)]
    (sort-by :updated_on
             (mapcat (fn [sublist-info]
                       (map #(assoc % :sublist_desc (:desc sublist-info))
                            (data/get-completed-items (:sublist_id sublist-info) completed-within-days)))
                     (if (:is_view list-info)
                       (data/get-view-sublists (auth/current-user-id) list-id)
                       [{:sublist_id list-id :desc (:desc list-info)}])))))

(defn- render-completed-item-list [list-id completed-within-days]
  (let [completed-items (completed-items list-id completed-within-days)]
    (if (= (count completed-items) 0)
      (render-empty-completion-list list-id)
      (map
       (fn [todo-item]
         [:div.item-row {:class (class-set
                                 {"high-priority" (> (:priority todo-item) 0)})}
          [:div.item-description
           [:div
            (todo-item/render-item-text (:desc todo-item))
            [:span.pill (:sublist_desc todo-item)]
            (when (> (:priority todo-item) 0)
              (if (todo-item/valentines-day?)
                img-heart-red
                img-star-yellow))]]])
       completed-items))))

(defn render-deleted-todo-list [list-id]
  [:div.empty-list
   [:h1
    "This list has been deleted!"]
   [:p
    "To view its contents, you must first restore it."]
   (hiccup-form/form-to
    [:post (shref "/list/" (encode-list-id list-id) "/restore")]
    [:input {:type "submit" :value "Restore List"}])])

(defn render-todo-list-delayed-page [list-id]
  (let [list-href (str "/list/" (encode-list-id list-id))]
    (render-page {:title ((data/get-todo-list-by-id list-id) :desc)
                  :client-redirect-time 5
                  :client-redirect list-href
                  :page-data-class "todo-list-delayed"}
                 [:div.empty-list
                  [:h1
                   "Waiting to display list."]
                  [:p
                   "Please wait for the contents of this list. Or click "
                   [:a {:href list-href :data-shortcut-key "0"
                        :data-turbo "false"}
                    "here"]
                   " to display now."]])))

(defn render-todo-list-completions-page [list-id params]
  (let [min-list-priority (or (try-parse-integer (:min-list-priority params)) 0)
        completed-within-days (or (try-parse-integer (:clwithin params)) 1)]
    (render-page {:title ((data/get-todo-list-by-id list-id) :desc)
                  :page-data-class "todo-list-completions"
                  :sidebar (sidebar/render-sidebar list-id min-list-priority 0)}
                 (scroll-column
                  "todo-list-completion-scroller"
                  [:h3
                   [:a {:href (shref (str "/list/" (encode-list-id list-id)) {:view :remove})}
                    img-back-arrow]
                   "Items Completed Since: " (todo-item/format-date (add-days (current-time) (- completed-within-days)))]
                  [:div.toplevel-list
                   (render-completed-item-list list-id completed-within-days)
                   (render-todo-list-completion-query-settings list-id completed-within-days)]))))

(defn render-todo-list-page [selected-list-id params]
  (let [edit-item-id (:edit-item-id params)
        min-list-priority (or (try-parse-integer (:min-list-priority params)) 0)
        min-item-priority (or (try-parse-integer (:min-item-priority params)) 0)
        completed-within-days (or (try-parse-integer (:cwithin params)) 0)
        snoozed-for-days (or (try-parse-integer (:sfor params)) 0)
        last-item-list-id  (:last-item-list-id params)
        list-info (data/get-todo-list-by-id selected-list-id)]
    (render-page {:title (:desc list-info)
                  :page-data-class "todo-list"
                  :sidebar (sidebar/render-sidebar selected-list-id min-list-priority snoozed-for-days)
                  :modals {"snoozing" #(modals/render-snooze-modal params selected-list-id)
                           "update-from" #(modals/render-update-from-modal params selected-list-id)
                           "share-with" #(modals/render-share-with-modal params selected-list-id)
                           "details" #(todo-list-details/render-todo-list-details-modal selected-list-id)
                           "delete-list" #(modals/render-list-delete-modal selected-list-id)
                           "sort-list" #(modals/render-list-sort-modal selected-list-id)}}
                 (if (:is_deleted list-info)
                   (render-deleted-todo-list selected-list-id)
                   (render-todo-list selected-list-id edit-item-id true completed-within-days snoozed-for-days last-item-list-id min-item-priority)))))

(defn render-todo-list-public-page [params]
  (let [list-id (decode-list-id (:list-id params))]
    (when (and (data/list-public? list-id)
               (not (data/list-owned-by-user-id? list-id (auth/current-user-id))))
      (render-page {:title ((data/get-todo-list-by-id list-id) :desc)
                    :page-data-class "todo-list"}
                   (render-todo-list list-id nil false 0 0 nil 0)))))

(defn render-todo-list-counts [params]
  (let [list-id (decode-list-id (:list-id params))
        list-info (and list-id (data/get-todo-list-by-id list-id))]
    (when (and list-info (not (:is_deleted list-info)))
      (let [is-view (:is_view list-info)]
        {:body {:active-items (if is-view
                                (data/get-todo-list-view-item-count list-id false)
                                (data/get-todo-list-item-count list-id false))
                :active-starred-items (if is-view
                                (data/get-todo-list-view-item-count list-id false 1)
                                (data/get-todo-list-item-count list-id false 1))
                :total-items (if is-view
                               (data/get-todo-list-view-item-count list-id true)
                               (data/get-todo-list-item-count list-id true))}}))))
