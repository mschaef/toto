(ns toto.todo
  (:use toto.util
        compojure.core
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

(defn js-link [ js-fn-name args & contents ]
  [:a {:href (str "javascript:" js-fn-name "(" args ")")} contents])

(def img-edit-item [:img { :src "/pen_alt_fill_12x12.png" :width 12 :height 12 :alt "Edit Item"}])
(def img-edit-list [:img { :src "/pen_alt_fill_12x12.png" :width 12 :height 12 :alt "Edit List Name"}])

(defn render-new-item-form [ list-id ]
  (form/form-to [:post (str "/list/" list-id)]
                (form/text-field { :class "full-width simple-border" :maxlength "1024" } "item-description")))

(defn string-leftmost [ string count ]
  (let [length (.length string)
        leftmost (min count length)]
    (if (< leftmost length)
      (str (.substring string 0 leftmost) "...")
      string)))

(defn shorten-url-text [ url-text ]
  (let [url (java.net.URL. url-text)
        base (str (.getProtocol url)
                  ":"
                  (if-let [authority (.getAuthority url)]
                    (str "//" authority)))]
    (str base
         (string-leftmost (.getPath url)
                          (max 0 (- 60 (.length base)))))))

(defn render-todo-item [ item-info item-number ]
  [:tr.item-row 
   (assoc-if { :valign "center" :itemid (item-info :item_id) }
             (= item-number 0)
             :class "first-row")
   [:td.item-control
    [:div { :id (str "item_control_" (item-info :item_id))}
     (complete-item-button item-info)]]
   [:td.item-description
    (let [desc (item-info :desc)
          clean-desc (hiccup.util/escape-html desc)]
      (list
       [:div { :id (str "item_desc_" (item-info :item_id)) :class "hidden"}
        clean-desc]
       [:div { :id (str "item_" (item-info :item_id))}
        (if (is-link-url? desc)
          [:a { :href desc :target "_blank" } (shorten-url-text clean-desc)]
          clean-desc)]))]])

(defn render-todo-list [ list-id ]
  [:table.item-list
   [:tr [:td { :colspan 2 } (render-new-item-form list-id)]]
   (map render-todo-item
        (data/get-pending-items list-id)
        (range))])

(defn render-todo-list-list [ selected-list-id ]
  [:div.full-width
   [:ul.list-list
    (map (fn [ list-info ]
           [:li (if (= (list-info :todo_list_id) (Integer. selected-list-id))
                  { :class "selected" :listid (list-info :todo_list_id) }
                  { :listid (list-info :todo_list_id) })
            [:a {:href (str "/list/" (list-info :todo_list_id) "/details")} img-edit-list]
            "&nbsp;"
            [:span { :id (str "list_" (list-info :todo_list_id) ) }
             [:div { :id (str "list_desc_" (list-info :todo_list_id)) :class "hidden"} 
              (hiccup.util/escape-html (list-info :desc))]
             [:a {:href (str "/list/" (list-info :todo_list_id))}
              (hiccup.util/escape-html (list-info :desc))
              " ("
              (list-info :item_count)
              ")"]]])
         (data/get-todo-lists-by-user (current-user-id)))]
   [:p.new-list
    (js-link "beginListCreate" nil "Add Todo List...")]])

(defn render-item-set-list-form []
    (form/form-to { :class "embedded" :id (str "item_set_list_form") } [:post "/item-list"]
                  (form/hidden-field "target-item")
                  (form/hidden-field "target-list")))

(defn render-todo-list-page [ selected-list-id ]
  (view/render-page { :page-title ((data/get-todo-list-by-id selected-list-id) :desc)
                     :include-js [ "/toto-todo-list.js" ]
                     :sidebar (render-todo-list-list selected-list-id)}
                    (render-item-set-list-form)
                    (render-todo-list selected-list-id)))

(defn render-todo-list-details-page [ list-id & { :keys [ error-message ]}]
  (let [ list-name ((data/get-todo-list-by-id list-id) :desc)
        list-owners (data/get-todo-list-owners-by-list-id list-id) ]
    (view/render-page { :page-title (str "List Details: " list-name) 
                       :sidebar (render-todo-list-list list-id) }
                      (form/form-to
                       [:post (str "/list/" list-id "/details")]
                       [:table.config-panel.full-width
                        [:tr
                         [:td.section-heading "List Name: "]
                         [:td (form/text-field { :class "full-width" :maxlength "32" } "list-name" list-name) ]]
                        [:tr
                         [:td.section-heading "List Owners: "]
                         [:td [:table.item-list
                               (map (fn [ { user-id :user_id user-email-addr :email_addr } ]
                                      (let [ user-parameter-name (str "user_" user-id)]
                                        [:tr.item-row
                                         [:td
                                          (if (= (current-user-id) user-id)
                                            (form/hidden-field user-parameter-name "on")
                                            (form/check-box user-parameter-name
                                                            (in? list-owners user-id)))]
                                         [:td.item-description user-email-addr]]))
                                    (data/get-friendly-users-by-id (current-user-id)))
                               [:tr
                                [:td]
                                [:td
                                 [:p.new-user
                                  (js-link "beginUserAdd" list-id "Add User To List...")]]]
                               (unless (empty? error-message)
                                 [:tr
                                  [:td { :colspan 2 }
                                   [:div#error error-message]]])]]]
                        [:tr
                         [:td]
                         [:td [:input {:type "submit" :value "Update List Details"}]]]]))))

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
                :markup (render-todo-list-details-page list-id :error-message "Invalid e-mail address")})
              
              (data/list-owned-by-user-id? list-id (user-info :user_id))
              (throw+
               { :type :form-error
                :markup (render-todo-list-details-page list-id :error-message "List already owned by this user.")})
              :else
              (user-info :user_id))))
         all-user-ids (clojure.set/union (apply hash-set selected-ids)
                                         (if (nil? email-user-id)
                                           #{}
                                           (hash-set email-user-id)))]

     (data/set-list-ownership list-id all-user-ids)
     (ring/redirect  (str "/list/" list-id "/details")))

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
  (GET "/" [] (redirect-to-home))

  (POST "/list" {{list-description :list-description} :params}
        (limit-string-length (add-list list-description) 32))

  (GET "/list/:list-id" [ list-id ]
       (render-todo-list-page list-id))

  (GET "/list/:list-id/details" [ list-id ]
       (render-todo-list-details-page list-id))

  (POST "/list/:list-id/details" { params :params }
        (let [ { list-id :list-id 
                list-name :list-name
                share-with-email :share-with-email }
               params ]

          (update-list-description list-id (limit-string-length list-name 32))
          (add-list-owner list-id share-with-email
                          (selected-user-ids-from-params params))))


  (POST "/list/:list-id/delete" { { list-id :list-id  } :params }
        (delete-list list-id))

  (POST "/list/:list-id" { { list-id :list-id
                            item-description :item-description }
                           :params }
        (add-item list-id (limit-string-length item-description 1024)))

  (POST "/item/:id"  {{id :id description :description} :params}
        (update-item id (limit-string-length description 1024)))

  (POST "/item-list"  {{ target-item :target-item target-list :target-list} :params}
        (update-item-list target-item target-list))

  (POST "/item/:id/complete" [id]
        (complete-item id))

  (POST "/item/:id/delete" [id]
        (delete-item id)))
