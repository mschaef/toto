(ns toto.todo.todo-list-manager
  (:use toto.core.util
        compojure.core
        toto.view.common
        toto.view.icons
        toto.view.components
        toto.view.query
        toto.view.page)
  (:require [hiccup.form :as form]
            [toto.data.data :as data]
            [toto.view.auth :as auth]
            [toto.todo.sidebar-view :as sidebar-view]))

(defn- list-priority-button [ list-id new-priority image-spec ]
  (post-button {:target (shref "/list/" list-id "/priority")
                :args {:new-priority new-priority}
                :desc "Set List Priority"}
               image-spec))

(defn render-list-star-control [ list-id priority ]
  (if (<= priority 0)
    (list-priority-button list-id 1 img-star-gray)
    (list-priority-button list-id 0 img-star-yellow)))

(defn render-list-arrow-control [ list-id priority ]
  (if (>= priority 0)
    (list-priority-button list-id -1 img-arrow-gray)
    (list-priority-button list-id 0 img-arrow-blue)))

(defn render-new-list-form [ ]
  (form/form-to
   {:class "new-item-form"}
   [:post (shref "/list")]
   (form/text-field {:maxlength "1024"
                     :placeholder "New List Name"
                     :autofocus "autofocus"}
                    "list-description")))

(defn render-list-list-page []
  (render-page
   {:page-title "Manage Todo Lists"}
   (scroll-column
    (render-new-list-form)
    [:div.toplevel-list.list-list
     (map (fn [ list ]
            (let [list-id (:todo_list_id list)
                  priority (:priority list)]
              [:div.item-row {:class (class-set {"high-priority" (> priority 0)
                                                 "low-priority" (< priority 0)})}
               [:div.item-control
                [:a {:href (shref "/list/" list-id "/details")} img-edit-list]]
               [:div.item-control
                (render-list-star-control list-id priority)]
               [:div.item-control
                (render-list-arrow-control list-id priority)]
               [:div.item-description
                [:a {:href (shref "/list/" list-id)}
                 (hiccup.util/escape-html (:desc list))
                 [:span.pill (:item_count list)]]
                (sidebar-view/render-list-visibility-flag list)]]))
          (data/get-todo-lists-by-user (auth/current-user-id)))])))

(defn render-todo-list-details-page [ list-id min-list-priority & { :keys [ error-message ]}]
  (let [list-details (data/get-todo-list-by-id list-id)
        list-name (:desc list-details)
        list-owners (data/get-todo-list-owners-by-list-id list-id) ]
    (render-page
     {:page-title (str "List Details: " list-name)
      :sidebar (sidebar-view/render-sidebar-list-list list-id min-list-priority 0)}

     (form/form-to
      {:class "details"}
      [:post (shref "/list/" list-id "/details")]
      [:div.config-panel
       [:h1 "List Views"]
       [:a {:href (shref "/stocking/" list-id) } "As Stocking"]
       [:a {:href (shref "/list/" list-id) } "As List"]]

       [:div.config-panel
        [:h1 "List Name:"]
        (form/text-field { :maxlength "32" } "list-name" list-name)]

       [:div.config-panel
        [:h1  "List Permissions:"]
        [:div
         (form/check-box "is_public" (:is_public list-details))
         [:label {:for "is_public"} "List publically visible?"]]]

       [:div.config-panel
        [:h1  "List Owners:"]
        [:div.list-owners
         (map (fn [ { user-id :user_id user-email-addr :email_addr } ]
                (let [ user-parameter-name (str "user_" user-id)]
                  [:div.list-owner
                   (if (= (auth/current-user-id) user-id)
                     [:div.self-owner
                      "&nbsp;"
                      (form/hidden-field user-parameter-name "on")]
                     (form/check-box user-parameter-name (in? list-owners user-id)))
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
            error-message])]]

       [:div.config-panel
        [:div
         [:input {:type "submit" :value "Update List Details"}]]]

       [:div.config-panel
        [:h1 "Sort List"]
        [:div
         [:input {:type "submit" :value "Sort By"
                  :formaction (shref "/list/" list-id "/sort")}]
         [:select {:id "sort-by" :name "sort-by"}
          (form/select-options [["Description" "desc"]
                                ["Created Date" "created-on"]
                                ["Updated Date" "updated-on"]
                                ["Snoozed Until" "snoozed-until"]])]]]

       [:div.config-panel
        [:h1 "Delete List"]
        (cond
          (<= (data/get-user-list-count (auth/current-user-id)) 1)
          [:span.warning "Your last list cannot be deleted."]

          (not (data/empty-list? list-id))
          [:span.warning "To delete this list, remove all items first."]

          :else
          (list
           [:div
            [:input.dangerous {:type "submit" :value "Delete List" :formaction (shref "/list/" list-id "/delete")}]
            [:span.warning "Warning, this cannot be undone."]]))]))))


