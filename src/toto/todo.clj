(ns toto.todo
  (:use compojure.core)
  (:require [ring.util.response :as ring]
            [hiccup.form :as form]
            [compojure.handler :as handler]
            [toto.data :as data]
            [toto.core :as core]
            [toto.view :as view]
            [toto.user :as user]))

(defn current-user-id []
  ((data/get-user-by-email core/*username*) :user_id))

(defn current-todo-list-id []
  (first (data/get-todo-list-ids-by-user (current-user-id))))

(defn redirect-to-home []
  (ring/redirect (str "/todo/" (current-todo-list-id))))

(defn complete-item-button [item-info]
  (form/form-to [:post (str "/item/" (item-info :item_id) "/complete")]
                (form/submit-button {} "Complete")))

(defn render-todo-list [ list-id ]
  [:table
   [:tr [:td "User"] [:td "Description"] [:td]]
   (map (fn [item-info]
          [:tr
           [:td (item-info :todo_list_id)]
           [:td [:a {:href (str "/item/" (item-info :item_id))} (item-info :desc)]]
           [:td (complete-item-button item-info)]])
        (data/get-pending-items list-id))
   [:tr [:td]
    [:td (form/form-to [:post "/item"]
                       (form/text-field {} "item-description"))]]])

(defn render-todo-list-page [ list-id ]
  (view/render-page
   (render-todo-list list-id)))

(defn add-item [item-description]
  (data/add-todo-item (current-todo-list-id)
                      item-description)
  (redirect-to-home))

(defn update-item [item-id item-description]
  (data/update-item-by-id item-id item-description)
  (redirect-to-home))

(defn complete-item [item-id]
  (data/complete-item-by-id item-id)
  (redirect-to-home))

(defn render-item [id]
  (let [item-info (data/get-item-by-id id)]
    (view/render-page [:h1 (str "Item: " id)]
                      (form/form-to [:post (str "/item/" id)]
                                    [:table
                                     [:tr [:td "Description:"] [:td (form/text-field {} "description" (item-info :desc))]]
                                     [:tr [:td "Completed:"] [:td (item-info :completed)]]]
                                    (form/submit-button {} "Update Item"))
                      [:a {:href "/"} "Home"])))

(defroutes all-routes
  (GET "/" []
       (redirect-to-home))

  (GET "/todo/:list-id" [ list-id ]
       (render-todo-list-page list-id))

  (POST "/item" {{item-description :item-description} :params}
        (add-item item-description))

  (GET "/item/:id" [id]
       (render-item id))

  (POST "/item/:id"  {{id :id description :description} :params}
        (update-item id description))

  (POST "/item/:id/complete" [id]
       (complete-item id)))
