(ns toto.todo.modals
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
            [toto.data.data :as data]
            [toto.view.auth :as auth]))

(defn render-snooze-modal [ params list-id ]
  (let [ snoozing-item-id (decode-item-id  (:snoozing-item-id params))]
    (defn render-snooze-choice [ label snooze-days shortcut-key ]
      (post-button {:desc (str label " (" shortcut-key ")")
                    :target (str "/item/" (encode-item-id snoozing-item-id) "/snooze")
                    :args {:snooze-days snooze-days}
                    :shortcut-key shortcut-key
                    :next-url (shref "/list/" (encode-list-id list-id) without-modal)}
                   (str label " (" shortcut-key ")")))
    (render-modal
     {:title "Snooze item until later"}
     [:div.snooze-choices
      (map (fn [ [ label snooze-days shortcut-key] ]
               (render-snooze-choice label snooze-days shortcut-key))
           [["Tomorrow" 1 "1"]
            ["In Three Days" 3 "2"]
            ["Next Week"  7 "3"]
            ["Next Month" 30 "4"]])]
     (when (:currently_snoozed (data/get-item-by-id snoozing-item-id))
       [:div.snooze-choices
        [:hr]
        (render-snooze-choice "Unsnooze" 0 "0")]))))

(defn- render-list-select [ id excluded-list-id ]
  [:select { :id id :name id }
   (hiccup-form/select-options
    (map (fn [ list-info ]
           [(hiccup-util/escape-html (:desc list-info))
            (:todo_list_id list-info)])
         (remove
          #(= excluded-list-id (:todo_list_id %))
          (data/get-todo-lists-by-user (auth/current-user-id)))))])

(defn render-update-from-modal [ params list-id ]
  (render-modal
   {:title "Update From"
    :form-post-to (shref "/list/" (encode-list-id list-id) "/copy-from" without-modal)}
   "Source:"
   (render-list-select "copy-from-list-id" (try-parse-integer list-id))
   [:div.modal-controls
    [:input {:type "submit" :value "Copy List"}]]))

(defn- render-todo-list-permissions [ list-id error-message ]
  (let [list-details (data/get-todo-list-by-id list-id)]
    (list
     [:div.config-panel
      [:h1  "List Permissions:"]
      [:div
       (hiccup-form/check-box "is-public" (:is_public list-details))
       [:label {:for "is-public"} "List publically visible?"]]]
     [:div.config-panel
      [:h1  "List Owners:"]
      (let [list-owners (data/get-todo-list-owners-by-list-id list-id) ]
        [:div.list-owners
         (map (fn [ { user-id :user_id user-email-addr :email_addr } ]
                (let [ user-parameter-name (str "user_" user-id)]
                  [:div.list-owner
                   (if (= (auth/current-user-id) user-id)
                     [:div.self-owner
                      "&nbsp;"
                      (hiccup-form/hidden-field user-parameter-name "on")]
                     (hiccup-form/check-box user-parameter-name (in? list-owners user-id)))
                   [:label {:for user-parameter-name}
                    user-email-addr
                    (when (= (auth/current-user-id) user-id)
                      [:span.pill "you"])]]))
              (data/get-friendly-users-by-id (auth/current-user-id)))
         [:div.list-owner
          [:div.self-owner "&nbsp;"]
          [:input {:id "share-with-email"
                   :name "share-with-email"
                   :type "text"
                   :placeholder "Share Mail Address"}]]
         (when error-message
           [:div.error-message
            error-message])])])))

(defn render-share-with-modal [ params list-id ]
  (render-modal
   {:title "Share With"
    :form-post-to (shref "/list/" (encode-list-id list-id) "/sharing" without-modal)}
   (render-todo-list-permissions list-id nil)
   [:div.modal-controls
    [:input {:type "submit" :value "Share"}]]))
