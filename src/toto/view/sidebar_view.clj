(ns toto.view.sidebar-view
  (:use toto.core.util
        toto.view.common)
  (:require [toto.data.data :as data]
            [toto.view.user :as user]))

(defn render-list-visibility-flag [ list ]
  (let [{is-public :is_public
         list-owner-count :list_owner_count}
        list]
    (cond
      is-public
      [:span.list-visibility-flag img-globe]

      (> list-owner-count 1)
      [:span.list-visibility-flag img-group])))

(defn render-sidebar-list-list [ selected-list-id snoozed-for-days]
  [:div.list-list
   (map (fn [ list ]
          (let [{list-id :todo_list_id
                 list-desc :desc
                 list-item-count :item_count
                 list-total-item-count :total_item_count
                 is-public :is_public
                 list-owner-count :list_owner_count}
                list ]
            [:div.list-row {:class (class-set {"selected" (= list-id (Integer. selected-list-id))})
                            :listid list-id}
             [:a.item {:href (shref "/list/" list-id)}
              (hiccup.util/escape-html list-desc)
              (render-list-visibility-flag list)]
             [:span.pill {:class (class-set {"highlight" (and snoozed-for-days
                                                              (not (= list-item-count list-total-item-count)))
                                             "emphasize" (not (= list-item-count list-total-item-count))})}
              (if snoozed-for-days
                list-total-item-count
                list-item-count)]]))
        (remove #(and (< (:priority %) 0)
                      (not (= (Integer. selected-list-id) (:todo_list_id %))))
                (data/get-todo-lists-by-user (current-user-id))))
   [:div.control-row
    [:a {:href "/lists"} "Manage Todo Lists"]]])
