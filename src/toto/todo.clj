(ns toto.todo
  (:use compojure.core
        [slingshot.slingshot :only (throw+ try+)])
  (:require [ring.util.response :as ring]
            [hiccup.form :as form]
            [hiccup.page :as page]
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

(defn string-empty? [ str ]
  (or (nil? str)
      (= 0 (count (.trim str)))))

(defn in? 
  "true if seq contains elm"
  [seq elm]  
  (some #(= elm %) seq))

(defn current-user-id []
  ((data/get-user-by-email (core/authenticated-username)) :user_id))

(defn current-todo-list-id []
  (first (data/get-todo-list-ids-by-user (current-user-id))))

(defn redirect-to-list [ list-id ]
  (ring/redirect (str "/list/" list-id)))

(defn redirect-to-home []
  (redirect-to-list (current-todo-list-id)))

(defn complete-item-button [ item-info ]
  (form/form-to [:post (str "/item/" (item-info :item_id) "/complete")]
                [:input {:type "image" :src "/check_12x10.png" :width 12 :height 10 :alt "Complete Item"}]))

(defn delete-item-button [ item-info ]
  (form/form-to [:post (str "/item/" (item-info :item_id) "/delete")]
                [:input {:type "image" :src "/x_11x11.png" :width 11 :height 11 :alt "Delete Item"}]))

(defn render-todo-list [ list-id ]
   [:table.item-list
    [:tr
     [:td {:colspan 1}]
     [:td 
      (form/form-to [:post (str "/list/" list-id)]
                    (form/text-field { :class "full-width" } "item-description"))]]
    (map (fn [item-info]
           [:tr.item-row { :valign "center" :itemid (item-info :item_id)}
            [:td
             (complete-item-button item-info)]
            [:td.item-description
              (let [desc (item-info :desc)]
                [:div { :id (str "item_" (item-info :item_id))}
                 [:a {:href (str "javascript:beginItemEdit(" (item-info :item_id) ")")}
                  [:img { :src "/pen_alt_fill_12x12.png" :width 12 :height 12 :alt "Edit Item"}]]
                 "&nbsp;"

                 (form/form-to { :class "embedded"
                                :id (str "item_set_list_" (item-info :item_id))
                                }
                               [:post (str "/item/" (item-info :item_id) "/list")]
                               (form/hidden-field "target-list"))

                 [:div { :id (str "item_desc_" (item-info :item_id)) :class "hidden"}
                  (hiccup.util/escape-html desc)]
                 (if (is-link-url? desc)
                   [:a { :href desc } (hiccup.util/escape-html desc)]
                   (hiccup.util/escape-html desc))])]])
         (data/get-pending-items list-id))])

(defn render-todo-list-list [ selected-list-id ]
  [:div.full-width
   [:ul.list-list
    (map (fn [ list-info ]
           [:li (if (= (list-info :todo_list_id) (Integer. selected-list-id))
                  { :class "selected" :listid (list-info :todo_list_id) }
                  { :listid (list-info :todo_list_id) })
            [:a {:href (str "/list/" (list-info :todo_list_id) "/sharing")}
             [:img { :src "/chat_alt_stroke_12x12.png" :width 12 :height 12 :alt "Share List"}]]
            "&nbsp;"
            [:span { :id (str "list_" (list-info :todo_list_id) ) }
             [:a {:href (str "javascript:beginListEdit(" (list-info :todo_list_id) ")")}
              [:img { :src "/pen_alt_fill_12x12.png" :width 12 :height 12 :alt "Edit List Name"}]]
             "&nbsp;"
             [:div { :id (str "list_desc_" (list-info :todo_list_id)) :class "hidden"} 
              (hiccup.util/escape-html (list-info :desc))]
             [:a {:href (str "/list/" (list-info :todo_list_id))}
              (hiccup.util/escape-html (list-info :desc))
              " ("
              (list-info :item_count)
              ")"]]])
         (data/get-todo-lists-by-user (current-user-id)))]
   [:p.new-list
    [:a { :href "javascript:beginListCreate()"} 
     "Add Todo List..."]]])

(defn render-todo-list-page [ selected-list-id ]
  (view/render-page  { :page-title ((data/get-todo-list-by-id selected-list-id) :desc) }
   (page/include-js "/toto-todo-list.js")
   [:div#sidebar
    (render-todo-list-list selected-list-id)]
   [:div#contents
    (render-todo-list selected-list-id)
    [:div { :class "list-control-footer"}
     [:a { :href (str "/list/" selected-list-id "/simple") } "[Simple Display]"]]]))

(defn render-todo-list-simply [ list-id ]
  (view/render-page { :page-title ((data/get-todo-list-by-id list-id) :desc) }
    (render-todo-list list-id)
    [:div { :class "list-control-footer"}
     [:a { :href (str "/list/" list-id) } "[Full Display]"]]))

(defn render-todo-list-sharing-page [ list-id & { :keys [ error-message ]}]
  (let [ list-name ((data/get-todo-list-by-id list-id) :desc)
        list-owners (data/get-todo-list-owners-by-list-id list-id) ]
    (view/render-page { :page-title 
                       (str "List Visibility: " ((data/get-todo-list-by-id list-id) :desc)) }
     [:div#sidebar
      (render-todo-list-list list-id)]
     [:div#contents
      (form/form-to
       [:post (str "/list/" list-id "/sharing")]
       [:table.item-list
        (map (fn [ user-info ]
               [:tr
                [:td
                 (if  (= (current-user-id) (user-info :user_id))
                   [:input { :type "hidden"
                            :name (str "user_" (user-info :user_id))
                            :value "on"}]
                   (if (in? list-owners (user-info :user_id))
                     [:input { :name (str "user_" (user-info :user_id))
                              :type "checkbox"
                              :checked "checked"}]
                     [:input { :name (str "user_" (user-info :user_id))
                              :type "checkbox" }]))]
                [:td.item-description
                 (user-info :email_addr)]])
             (data/get-friendly-users-by-id (current-user-id)))
        [:tr
         [:td]
         [:td
          [:p.new-user
           [:a { :href (str "javascript:beginUserAdd(" list-id ")")} 
            "Add User To List..."]]]]
        (if (not (empty? error-message))
          [:tr
           [:td { :colspan 2 }
            [:div#error error-message]]])
        [:tr
         [:td]
         [:td [:input {:type "submit" :value "Update Sharing"}]]]])])))


(defn update-list-description [ list-id list-description ]
  (when (not (string-empty? list-description))
    (data/update-list-description list-id list-description ))
  (ring/redirect  (str "/list/" list-id)))

(defn delete-list [ list-id ]
  (data/remove-list-owner list-id (current-user-id))
  (redirect-to-home))

(defn add-list-owner [ list-id share-with-email selected-ids ]
  (try+
   (let [ email-user-id
         (if (empty? share-with-email)
           nil
           (let [ user-info (data/get-user-by-email share-with-email)]
             (cond
              (nil? user-info)
              (throw+
               { :type :form-error
                :markup (render-todo-list-sharing-page list-id
                                               :error-message "Invalid e-mail address")})
              
              (data/list-owned-by-user-id? list-id (user-info :user_id))
              (throw+
               { :type :form-error
                :markup (render-todo-list-sharing-page list-id
                                               :error-message "List already owned by this user.")})

              :else
              (user-info :user_id))))
         all-user-ids (clojure.set/union (apply hash-set selected-ids)
                                         (if (nil? email-user-id)
                                           #{}
                                           (hash-set email-user-id)))]

     (data/set-list-ownership list-id all-user-ids)
     (ring/redirect  (str "/list/" list-id "/sharing")))

   (catch [ :type :form-error ] { :keys [ markup ]}
     markup)))

(defn add-list [ list-description ]
  (when (not (string-empty? list-description))
    (data/add-list-owner (current-user-id) (data/add-list list-description)))
  (redirect-to-home))

(defn add-item [ list-id item-description ]
  (when (not (string-empty? item-description))
    (data/add-todo-item list-id item-description))
  (redirect-to-list list-id))

(defn update-item [ item-id item-description ]
  (let [ list-id ((data/get-item-by-id item-id) :todo_list_id)]
    (when (not (string-empty? item-description))
      (data/update-item-by-id item-id item-description))
    (redirect-to-list list-id)))

(defn update-item-list [ item-id target-list-id ] 
  (let [ original-list-id ((data/get-item-by-id item-id) :todo_list_id)]
    (data/update-item-list item-id target-list-id)
    (redirect-to-list original-list-id)))

(defn complete-item [ item-id ]
  (let [ list-id ((data/get-item-by-id item-id) :todo_list_id)]
    (data/complete-item-by-id (current-user-id) item-id)
    (redirect-to-list list-id)))

(defn delete-item [ item-id ]
  (let [ list-id ((data/get-item-by-id item-id) :todo_list_id)]
    (data/delete-item-by-id (current-user-id) item-id)
    (redirect-to-list list-id)))

(defn selected-user-ids-from-params [ params ]
  (map #(Integer/parseInt (.substring % 5))
       (filter #(.startsWith % "user_") (map name (keys params)))))

(defroutes all-routes
  (GET "/" []
       (redirect-to-home))

  (POST "/list" {{list-description :list-description} :params}
        (add-list list-description))

  (GET "/list/:list-id" [ list-id ]
       (render-todo-list-page list-id))

  (GET "/list/:list-id/simple" [ list-id ]
       (render-todo-list-simply list-id))

  (GET "/list/:list-id/sharing" [ list-id ]
       (render-todo-list-sharing-page list-id))

  (POST "/list/:list-id/sharing" { params :params }
        (let [ { list-id :list-id 
                share-with-email :share-with-email }
               params ]

          (add-list-owner list-id share-with-email
                          (selected-user-ids-from-params params))))

  (POST "/list/:list-id/description" { { list-id :list-id description :description } :params }
        (update-list-description list-id description))

  (POST "/list/:list-id/delete" { { list-id :list-id  } :params }
        (delete-list list-id))

  (POST "/list/:list-id" { { list-id :list-id
                            item-description :item-description }
                           :params }
        (add-item list-id item-description))

  (POST "/item/:id"  {{id :id description :description} :params}
        (update-item id description))

  (POST "/item/:id/list"  {{id :id target-list :target-list} :params}
        (update-item-list id target-list))

  (POST "/item/:id/complete" [id]
        (complete-item id))

  (POST "/item/:id/delete" [id]
        (delete-item id)))
