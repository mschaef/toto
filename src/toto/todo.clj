(ns toto.todo
  (:use compojure.core)
  (:require [ring.util.response :as ring]
            [hiccup.form :as form]
            [compojure.handler :as handler]
            [toto.data :as data]
            [toto.core :as core]
            [toto.view :as view]
            [toto.user :as user]))

(defn is-link-url? [ text ]
  (try
   (let [url (java.net.URL. text)
         protocol (.getProtocol url)]
     (or (= protocol "http")
         (= protocol "https")))
   (catch java.net.MalformedURLException ex
     false)))

(defn current-user-id []
  ((data/get-user-by-email core/*username*) :user_id))

(defn current-todo-list-id []
  (first (data/get-todo-list-ids-by-user (current-user-id))))

(defn redirect-to-list [ list-id ]
  (ring/redirect (str "/list/" list-id)))

(defn redirect-to-home []
  (redirect-to-list (current-todo-list-id)))

(defn complete-item-button [item-info]
  (form/form-to [:post (str "/item/" (item-info :item_id) "/complete")]
                [:input {:type "image" :src "/check_16x13.png" :width 16 :height 13 :alt "Complete Item"}]))

(defn render-todo-list [ list-id ]
   [:table.item-list
    (map (fn [item-info]
           [:tr {:valign "center"}
            [:td
             (complete-item-button item-info)]
            [:td
             [:a {:href (str "/item/" (item-info :item_id))}
              [:img { :src "/pen_alt_fill_16x16.png" :width 16 :height 16 :alt "Edit Item"}]]]

            [:td.item-description
             (let [desc (item-info :desc)]
               (if (is-link-url? desc)
                 [:a { :href desc } desc]
                 desc))]])

         (data/get-pending-items list-id))
    [:tr
     [:td {:colspan 2}]
     [:td 
      (form/form-to [:post (str "/list/" list-id)]
                    (form/text-field { :class "full-width"  } "item-description"))]]])

(defn render-todo-list-list [ selected-list-id ]
   [:ul.list-list
    (map (fn [ list-info ]
           [:li (if (= (list-info :todo_list_id) (Integer. selected-list-id))
                  { :class "selected" }
                  { })
            [:a {:href (str "/list/" (list-info :todo_list_id))}
             (list-info :desc)]])
         (data/get-todo-lists-by-user (current-user-id)))
    [:li (form/form-to [:post "/list"]
                       (form/text-field { :class "full-width" } "list-description"))]])

(defn render-todo-list-page [ selected-list-id ]
  (view/render-page
   [:div#sidebar
    (render-todo-list-list selected-list-id)]
   [:div#contents
    (render-todo-list selected-list-id)
    [:a { :href (str "/list/" selected-list-id "/sharing") }  "Edit List Sharing"]]))

(defn in? 
  "true if seq contains elm"
  [seq elm]  
  (some #(= elm %) seq))

(defn render-todo-list-sharing-page [ list-id & { :keys [ error-message ]}]
  (view/render-page
   (let [ list-name ((data/get-todo-list-by-id list-id) :desc)
         list-owners (data/get-todo-list-owners-by-list-id list-id) ]
     [:div#contents
      [:h2 "List Owners: " 
            [:a {:href (str "/list/" list-id)}
             list-name]]
      (form/form-to
       [:post (str "/list/" list-id "/sharing")]
       [:table.item-list
        (map (fn [ user-info ]
               (if (not (= (current-user-id) (user-info :user_id)))
                 [:tr
                  [:td
                   (if (in?  list-owners (user-info :user_id))
                     [:input {:type "checkbox" :checked "checked"}]
                     [:input {:type "checkbox" }])]
                  [:td.item-description
                   (user-info :email_addr)]]))
             (data/get-friendly-users-by-id (current-user-id)))
        [:tr
         [:td]
         [:td
          (form/text-field { :class "full-width"  }
                           "share-with-email")]]
        (if (not (empty? error-message))
          [:tr
           [:td { :colspan 2 }
            [:div#error error-message]]])
        [:tr
         [:td]
         [:td [:input {:type "submit" :value "Update Sharing"}]]]])])))


(defn add-list-owner [ list-id share-with-email ]
  (let [ user-info (data/get-user-by-email share-with-email)]
    (cond
     (nil? user-info)
     (render-todo-list-sharing-page list-id
                                    :error-message "Invalid e-mail address")

     (data/list-owned-by-user-id? list-id (user-info :user_id))
     (render-todo-list-sharing-page list-id
                                    :error-message "List already owned by this user.")

     :else
     (do
       (data/add-list-owner (user-info :user_id) list-id)
       (ring/redirect  (str "/list/" list-id "/sharing"))))))


(defn add-list [ list-description ]
  (let [ list-id (data/add-list list-description) ]
    (data/add-list-owner (current-user-id) list-id)
    (redirect-to-home)))

(defn add-item [ list-id item-description ]
  (data/add-todo-item list-id item-description)
  (redirect-to-list list-id))

(defn update-item [ item-id item-description ]
  (data/update-item-by-id item-id item-description)
  (redirect-to-home))

(defn complete-item [ item-id ]
  (data/complete-item-by-id (current-user-id) item-id)
  (redirect-to-home))

(defn render-item [ id ]
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

  (POST "/list" {{list-description :list-description} :params}
        (add-list list-description))

  (GET "/list/:list-id" [ list-id ]
       (render-todo-list-page list-id))

  (GET "/list/:list-id/sharing" [ list-id ]
       (render-todo-list-sharing-page list-id))

  (POST "/list/:list-id/sharing" {{list-id :list-id 
                                   share-with-email :share-with-email}
                                  :params}
        (add-list-owner list-id share-with-email))

  (POST "/list/:list-id" {{list-id :list-id item-description :item-description} :params}
        (add-item list-id item-description))

  (GET "/item/:id" [id]
       (render-item id))

  (POST "/item/:id"  {{id :id description :description} :params}
        (update-item id description))

  (POST "/item/:id/complete" [id]
       (complete-item id)))
