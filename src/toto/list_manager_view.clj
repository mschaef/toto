(ns toto.list-manager-view
  (:use toto.util
        compojure.core
        toto.view-utils)
  (:require [hiccup.form :as form]
            [toto.data :as data]
            [toto.view :as view]
            [toto.sidebar-view :as sidebar-view]
            [toto.user :as user]))

(defn render-new-list-form [ ]
  (form/form-to
   {:class "new-item-form"}
   [:post (shref "/list")]
   (form/text-field {:class "full-width simple-border"
                     :maxlength "1024"
                     :placeholder "New List Name"
                     :autofocus "autofocus"}
                    "list-description")))

(defn render-list-list-page []
  (view/render-page
   {:page-title "Manage Todo Lists"}
   (render-scroll-column
    (render-new-list-form)
    [:div.toplevel-list
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
               [:div.item
                [:a {:href (shref "/list/" list-id)}
                 (hiccup.util/escape-html (:desc list))]
                [:span.pill (:item_count list)]
                (sidebar-view/render-list-visibility-flag list)]]))
          (data/get-todo-lists-by-user (current-user-id)))])))

(defn render-todo-list-details-page [ list-id & { :keys [ error-message ]}]
  (let [list-details (data/get-todo-list-by-id list-id)
        list-name (:desc list-details)
        list-owners (data/get-todo-list-owners-by-list-id list-id) ]
    (view/render-page
     {:page-title (str "List Details: " list-name)
      :sidebar (sidebar-view/render-sidebar-list-list list-id 0)}
     (form/form-to
      {:class "details"}
      [:post (shref "/list/" list-id "/details")]
       [:div.config-panel
        [:h1 "List Name:"]
        (form/text-field { :class "full-width simple-border" :maxlength "32" }
                                          "list-name" list-name)]

       [:div.config-panel
        [:h1  "List Permissions:"]
        (form/check-box "is_public" (:is_public list-details))
        [:label {:for "is_public"} "List publically visible?"]]

       [:div.config-panel
        [:h1  "List Owners:"]
        [:div.list-owners
         (map (fn [ { user-id :user_id user-email-addr :email_addr } ]
                (let [ user-parameter-name (str "user_" user-id)]
                  [:div.list-owner
                   (if (= (current-user-id) user-id)
                     [:div.self-owner
                      "&nbsp;"
                      (form/hidden-field user-parameter-name "on")]
                     (form/check-box user-parameter-name (in? list-owners user-id)))
                   [:label {:for user-parameter-name}
                    user-email-addr
                    (when (= (current-user-id) user-id)
                      [:span.pill "you"])]]))
              (data/get-friendly-users-by-id (current-user-id)))]]

       [:div.config-panel
        [:input {:type "submit" :value "Update List Details"}]]

       [:div.config-panel
        [:h1  "View List"]
        [:a { :href (shref "/list/" list-id) } "View List"]]

       [:div.config-panel
        [:h1  "Download List"]
        [:a { :href (shref "/list/" list-id "/list.csv" ) } "Download List as CSV"]]

       [:div.config-panel
        [:h1  "Delete List"]
        (if (data/empty-list? list-id)
          (list
           [:input.dangerous {:type "submit" :value "Delete List" :formaction (shref "/list/" list-id "/delete")}]
           [:span.warning "Warning, this cannot be undone."])
          [:span.warning "To delete this list, remove all items first."])]))))
