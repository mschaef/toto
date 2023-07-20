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

(ns toto.todo.todo-item
  (:use playbook.core
        toto.view.common
        toto.view.icons
        toto.view.components
        toto.view.query
        toto.view.page
        toto.todo.ids)
  (:require [taoensso.timbre :as log]
            [hiccup.util :as hiccup-util]
            [toto.view.auth :as auth]))

(defn valentines-day? []
  (let [now (current-time)]
    (and (= 1 (.getMonth now))
         (let [day (.getDate now)]
           (or (= day 14) (= day 15))))))

;;;; Item text rendering. This is the textual representation of a todo
;;;; item, without any surrounding controls.

(def html-breakpoint "&#8203;")

(defn- ensure-string-breakpoints [ s n ]
  (clojure.string/join html-breakpoint (map hiccup-util/escape-html (partition-string s n))))

(defn- ensure-string-breaks [ string at ]
  (clojure.string/replace string at (str at html-breakpoint)))

(def url-regex #"(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")

(defn- render-url [ config url-text ]
  (let [target-length (:url-target-length config)
        url (java.net.URL. url-text)
        base (str (.getProtocol url)
                  ":"
                  (if-let [authority (.getAuthority url)]
                    (str "//" authority "/")))
        is-self-link? (= base (:base-url config))
        url-text (-> (hiccup-util/escape-html
                      (str base
                           (string-leftmost (.getPath url)
                                            (max 0 (- (- target-length 3) (.length base)))
                                            "...")))
                     (ensure-string-breaks "/")
                     (ensure-string-breaks "."))]
    [:a.item-link (cond-> { :href url }
                    (not is-self-link?) (merge {:target "_blank"} ))
     url-text]))

(defn- render-item-text-segment [ item-text-segment ]
  (clojure.string/join " " (map #(ensure-string-breakpoints % 15)
                                (clojure.string/split item-text-segment #"\s"))))

;;;; The full item view, including controls

(def pill-date-format (java.text.SimpleDateFormat. "yyyy-MM-dd"))

(defn format-date [ date ]
  (.format pill-date-format date))

(defn render-item-text [ config item-text ]
  (interleave (conj (vec (map #(str " " (render-item-text-segment (.trim %)) " ")
                              (clojure.string/split item-text url-regex))) "")
              (conj (vec (map #(render-url config %) (map first (re-seq url-regex item-text)))) "")))


(defn- complete-item-button [ item-info ]
  (post-button {:desc "Complete Item"
                :target (str "/item/" (encode-item-id (item-info :item_id)) "/complete")}
               img-check))

(defn- restore-item-button [ item-info ]
  (post-button {:desc "Restore Item"
                :target (str "/item/" (encode-item-id (item-info :item_id)) "/restore")}
               img-restore))

(defn- delete-item-button [ item-info list-id ]
  (post-button {:desc "Delete Item"
                :target (str "/item/" (encode-item-id (item-info :item_id)) "/delete")}
               img-trash))

(defn- snooze-item-button [ item-info body ]
  [:a {:href (shref "" {:modal "snoozing"
                        :snoozing-item-id (encode-item-id (item-info :item_id))})}
   body])

(defn- item-priority-button [ item-id new-priority image-spec writable? ]
  (if writable?
    (post-button {:target (str "/item/" item-id "/priority")
                  :args {:new-priority new-priority}
                  :desc "Set Item Priority"}
                 image-spec)
    image-spec))

(defn- render-item-priority-control [ item-id priority writable? ]
  (if (valentines-day?)
    (if (<= priority 0)
      (item-priority-button item-id 1 img-heart-pink writable?)
      (item-priority-button item-id 0 img-heart-red writable?))
    (if (<= priority 0)
      (item-priority-button item-id 1 img-star-gray writable?)
      (item-priority-button item-id 0 img-star-yellow writable?))))

(defn- render-age [ days ]
  (cond (> days 720) (str (quot days 360) "y")
        (> days 60) (str (quot days 30) "m")
        :else (str days "d")))

(defn- item-drag-handle [ class item-info ]
  [:div.item-drag-handle {:itemid (:item_id item-info)
                          :class class}
   img-drag-handle])

(defn- drop-target [ item-ordinal ]
  [:div.order-drop-target {:ordinal item-ordinal :priority "0"} "&nbsp;"])

(defn render-todo-item [ config view-list-id list-id item-info writable? editing? max-item-age ]
  (let [{item-id :item_id
         is-complete? :is_complete
         is-deleted? :is_deleted
         priority :priority
         snoozed-until :snoozed_until
         currently-snoozed :currently_snoozed
         created-by-id :created_by_id
         created-by-email :created_by_email
         created-by-name :created_by_name}
        item-info]
    [:div.item-row.order-drop-target
     (cond-> {:id (str "item_row_" item-id)
              :itemid item-id
              :listid list-id
              :ordinal (:item_ordinal item-info)
              :priority priority
              :class (class-set {"editing" editing?
                                 "display" (not editing?)
                                 "high-priority" (> priority 0)
                                 "snoozed" currently-snoozed})}
       writable? (assoc :edit-href (shref "/list/" (encode-list-id view-list-id)
                                          { :edit-item-id (encode-item-id item-id) })))
     (when writable?
       (list
        (item-drag-handle "left" item-info)
        [:div.item-control.complete {:id (str "item_control_" item-id)}
         (if editing?
           (delete-item-button item-info list-id)
           (if (or is-complete? is-deleted?)
             (restore-item-button item-info)
             (complete-item-button item-info)))]))
     [:div.item-control.priority.left
      (render-item-priority-control item-id priority writable?)]
     [:div.item-description {:itemid item-id}
      (if editing?
         [:input (cond-> {:value (item-info :desc)
                          :type "text"
                          :name "description"
                          :item-id item-id
                          :view-href (shref "/list/" (encode-list-id view-list-id) without-modal)
                          :onkeydown "window._toto.onItemEditKeydown(event)"}
                   editing? (assoc "autofocus" "on"))]
        (let [desc (item-info :desc)]
          [:div {:id (str "item_" item-id)
                 :class (class-set {"deleted-item" is-deleted?
                                    "completed-item" is-complete?})}
           (render-item-text config desc)
           (snooze-item-button item-info
                               (if (> (:sunset_age_in_days item-info) (- max-item-age 3))
                                 img-sunset
                                 [:span.pill
                                  (render-age (:age_in_days item-info))
                                  (when currently-snoozed
                                    (list
                                     ", snoozed: " (format-date snoozed-until)))]))
           (when (not (= created-by-id (auth/current-user-id)))
             [:span.pill { :title created-by-email }
              (hiccup-util/escape-html
               created-by-name)])]))]
     [:div.item-control.priority.right
      (render-item-priority-control item-id priority writable?)]
     (item-drag-handle "right" item-info)]))
