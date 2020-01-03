(ns toto.sidebar-view
  (:use toto.util
        toto.view-utils)
  (:require [toto.data :as data]
            [toto.user :as user]))

(defn render-sidebar-list-list [ selected-list-id ]
  [:div.list-list
   (map (fn [ { list-id :todo_list_id list-desc :desc list-item-count :item_count is-public :is_public list-owner-count :list_owner_count} ]
          [:div.list-row {:class (class-set {"selected" (= list-id (Integer. selected-list-id))})
                          :listid list-id}

           [:a.item {:href (shref "/list/" list-id)}
            (hiccup.util/escape-html list-desc)
                       (when (> list-owner-count 1)
             [:span.group-list-flag img-group])
           (when is-public
             [:span.pill.public-flag
              "public"])]
           [:span.pill list-item-count]])
        (remove #(and (< (:priority %) 0)
                      (not (= (Integer. selected-list-id) (:todo_list_id %))))
                (data/get-todo-lists-by-user (current-user-id))))
   [:div.control-row
    [:a {:href "/lists"} "Manage Todo Lists"]]])
