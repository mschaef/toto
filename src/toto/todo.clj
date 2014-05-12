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

(defn current-user-id []
  ((data/get-user-by-email (core/authenticated-username)) :user_id))

(defn current-todo-list-id []
  (first (data/get-todo-list-ids-by-user (current-user-id))))

(defn ensure-list-access [ list-id ]
  (unless (data/list-owned-by-user-id? list-id (current-user-id))
          (core/report-unauthorized)))

(defn ensure-item-access [ item-id ]
  (unless (data/item-owned-by-user-id? item-id (current-user-id))
          (core/report-unauthorized)))

(defn redirect-to-list [ list-id ]
  (ring/redirect (str "/list/" list-id)))

(defn redirect-to-home []
  (redirect-to-list (current-todo-list-id)))

(defn complete-item-button [ item-info ]
  (form/form-to [:post (str "/item/" (item-info :item_id) "/complete")]
                [:input {:type "image" :src "/check_12x10.png" :width 12 :height 10 :alt "Complete Item"}]))

(defn restore-item-button [ item-info ]
  (form/form-to [:post (str "/item/" (item-info :item_id) "/restore")]
                [:input {:type "image" :src "/reload_12x14.png" :width 12 :height 14 :alt "Restore Item"}]))

(defn delete-item-button [ item-info ]
  (form/form-to [:post (str "/item/" (item-info :item_id) "/delete")]
                [:input {:type "image" :src "/x_11x11.png" :width 11 :height 11 :alt "Delete Item"}]))

(defn item-priority-button [ item-id new-priority image-spec ]
  (form/form-to [:post (str "/item/" item-id "/priority")]
                [:input (assoc image-spec :type "image") ]
                (form/hidden-field "new-priority" new-priority)))

(defn js-link [ js-fn-name args & contents ]
  [:a {:href (str "javascript:" js-fn-name "(" args ")")} contents])

(def img-edit-list { :src "/pen_alt_fill_12x12.png" :width 12 :height 12 :alt "Edit List"})
(def img-edit-list-light { :src "/pen_alt_fill_12x12_light.png" :width 12 :height 12 :alt "Edit List"})
(def img-star-gray { :src "/star_gray_16x16.png" :width 16 :height 16 :alt "Elevate Priority"})
(def img-star-yellow { :src "/star_yellow_16x16.png" :width 16 :height 16 :alt "Restore Priority"})
(def img-arrow-down-gray { :src "/arrow_down_gray_16x16.png" :width 16 :height 16 :alt "Lower Priority"})
(def img-arrow-down-blue { :src "/arrow_down_blue_16x16.png" :width 16 :height 16 :alt "Restore Priority"})

(defn image [ image-spec ]
  [:img image-spec])

(defn render-new-item-form [ list-id ]
  (form/form-to [:post (str "/list/" list-id)]
                (form/text-field { :class "full-width simple-border" :maxlength "1024" } "item-description")))


(def url-regex #"(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]") 

(defn render-url [ [ url ] ]
  [:a { :href url :target "_blank" }
      (hiccup.util/escape-html (shorten-url-text url 60))])

(defn render-item-text [ item-text ]
  (interleave (conj (clojure.string/split item-text url-regex) "")
              (conj (vec (map render-url (re-seq url-regex item-text))) "")))

(defn render-age [ days ]
  (cond (> days 720) (str (quot days 360) "y")
        (> days 60) (str (quot days 30) "m")
        :else (str days "d")))

(defn render-todo-item [ item-info item-number ]
  (let [ {
          item-id :item_id
          item-desc :desc
          item-age :age_in_days
          completed-on :completed_on
          is-delete? :is_delete
          priority :priority
          }
         item-info]

    [:tr.item-row 
     (assoc-if {:itemid item-id} (= item-number 0) :class "first-row")
     [:td.item-control
      [:div { :id (str "item_control_" item-id)}
       (if (nil? completed-on)
         (complete-item-button item-info)
         (restore-item-button item-info))]]
     [:td.item-description
      (let [desc (item-info :desc)]
        (list
         [:div { :id (str "item_desc_" item-id) :class "hidden"}
          (hiccup.util/escape-html desc)]
         [:div (assoc-if { :id (str "item_" item-id)}
                          (not (nil? completed-on))
                          :class (if is-delete? "deleted_item" "completed_item"))
          (render-item-text desc)
          [:span#item_age
           " (" (render-age item-age) ")"]]))]
     [:td.item-control
      (if (<= priority 0)
        (item-priority-button item-id 1 img-star-gray)
        (item-priority-button item-id 0 img-star-yellow))]
     [:td.item-control
      (if (>= priority 0)
        (item-priority-button item-id -1 img-arrow-down-gray)
        (item-priority-button item-id 0 img-arrow-down-blue))]]))

(defn render-todo-list [ list-id completed-within-days ]
  [:table.item-list
   [:tr
    [:td { :colspan 4 }
     (render-new-item-form list-id)]]
   (map render-todo-item
        (data/get-pending-items list-id completed-within-days )
        (range))])

(defn render-todo-list-list [ selected-list-id ]
  [:table.list-list
    (map (fn [ { list-id :todo_list_id list-desc :desc list-item-count :item_count } ]
           [:tr (if (= list-id (Integer. selected-list-id))
                  { :class "selected" :listid list-id }
                  { :listid list-id })

            [:td.item-control
             [:a {:href (str "/list/" list-id "/details")}
              (if (core/is-mobile-request?)
                (image img-edit-list-light)
                (image img-edit-list))]]
            [:td
             [:span { :id (str "list_" list-id ) }
              [:div { :id (str "list_desc_" list-id) :class "hidden"} 
               (hiccup.util/escape-html list-desc)]
              [:a {:href (str "/list/" list-id)}
               (hiccup.util/escape-html list-desc) " (" list-item-count ")"]]]])
         (data/get-todo-lists-by-user (current-user-id)))
   [:tr
    [:td.add-list  { :colspan "2"}
     (js-link "beginListCreate" nil "Add Todo List...")]]])

(defn render-item-set-list-form []
    (form/form-to { :class "embedded" :id (str "item_set_list_form") } [:post "/item-list"]
                  (form/hidden-field "target-item")
                  (form/hidden-field "target-list")))

(defn render-todo-list-page [ selected-list-id completed-within-days ]
  (view/render-page { :page-title ((data/get-todo-list-by-id selected-list-id) :desc)
                     :include-js [ "/toto-todo-list.js" ]
                     :sidebar (render-todo-list-list selected-list-id)}
                    (render-item-set-list-form)
                    (render-todo-list selected-list-id completed-within-days )
                    [:div.query-settings
                     "Include items completed within: "
                     (form/form-to { :class "embedded "} [ :get (str "/list/" selected-list-id)]
                                   [:select { :id "cwithin" :name "cwithin" } 
                                    (form/select-options [ [ "-" "-"] [ "1d" "1"] [ "7d" "7"] ]
                                                         (if (nil? completed-within-days) "-" (str completed-within-days)))
                                    (form/submit-button "Query")])]))

(defn config-panel [ target-url & sections ]
  (form/form-to
   [:post target-url]
   [:table.config-panel
    (map (fn [ [ heading body ] ]
           [:tr
            [:td.section-heading heading]
            [:td body]])
         sections)]))

(defn render-todo-list-details-page [ list-id & { :keys [ error-message ]}]
  (let [ list-name ((data/get-todo-list-by-id list-id) :desc)
        list-owners (data/get-todo-list-owners-by-list-id list-id) ]
    (view/render-page
     { :page-title (str "List Details: " list-name) 
      :sidebar (render-todo-list-list list-id) }
     (config-panel
      (str "/list/" list-id "/details")

      ["List Name:" (form/text-field { :class "full-width" :maxlength "32" } "list-name" list-name)]

      ["List Owners:" [:table.item-list
                       (map (fn [ { user-id :user_id user-email-addr :email_addr } ]
                              (let [ user-parameter-name (str "user_" user-id)]
                                [:tr.item-row
                                 [:td.item-control
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
                                 [:div#error error-message]]])]]

      [ nil [:input {:type "submit" :value "Update List Details"}]])

     (config-panel
      (str "/list/" list-id "/delete")
      ["Delete List: " (if (data/empty-list? list-id)
                         (list 
                          [:input.dangerous {:type "submit" :value "Delete List"}]
                          [:span#warning "Warning, this cannot be undone."])
                         [:span#warning "To delete this list, remove all items first."])]))))

(defn update-list-description [ list-id list-description ]
  (when (not (string-empty? list-description))
    (data/update-list-description list-id list-description ))
  (ring/redirect  (str "/list/" list-id)))

(defn delete-list [ list-id ]
  (data/delete-list list-id)
  (redirect-to-home))

(defmacro catch-validation-errors [ & body ]
  `(try+
    ~@body
    (catch [ :type :form-error ] details#
      ( :markup details#))))

(defn fail-validation [ markup ]
  (throw+ { :type :form-error :markup markup }))

(defn get-user-id-by-email [ email ]
  (if-let [ user-info (data/get-user-by-email email) ]
    (user-info :user_id)
    nil))

(defn add-list-owner [ list-id share-with-email selected-ids ]
  (catch-validation-errors
   (let [ email-user-id
         (if (empty? share-with-email)
           nil
           (or (get-user-id-by-email share-with-email)
               (fail-validation
                (render-todo-list-details-page list-id :error-message "Invalid e-mail address"))))]

     (data/set-list-ownership list-id
                              (clojure.set/union (apply hash-set selected-ids)
                                                 (if (nil? email-user-id)
                                                   #{}
                                                   (hash-set email-user-id))))
     (ring/redirect  (str "/list/" list-id "/details")))))

(defn add-list [ list-description ]
  (when (not (string-empty? list-description))
    (data/add-list-owner (current-user-id) (data/add-list list-description)))
  (redirect-to-home))

(defn add-item [ list-id item-description ]
  (when (not (string-empty? item-description))
    (data/add-todo-item list-id item-description))
  (redirect-to-list list-id))

(defn update-item-desc [ item-id item-description ]
  (let [ list-id ((data/get-item-by-id item-id) :todo_list_id)]
    (when (not (string-empty? item-description))
      (data/update-item-desc-by-id item-id item-description))
    (redirect-to-list list-id)))

(defn update-item-list [ item-id target-list-id ] 
  (let [ original-list-id ((data/get-item-by-id item-id) :todo_list_id)]
    (data/update-item-list item-id target-list-id)
    (redirect-to-list original-list-id)))

(defn update-item-priority [ item-id new-priority ]
  (let [ original-list-id ((data/get-item-by-id item-id) :todo_list_id)]
    (data/update-item-priority-by-id item-id new-priority)
    (redirect-to-list original-list-id)))

(defn complete-item [ item-id ]
  (let [ list-id ((data/get-item-by-id item-id) :todo_list_id)]
    (data/complete-item-by-id (current-user-id) item-id)
    (redirect-to-list list-id)))

(defn delete-item [ item-id ]
  (let [ list-id ((data/get-item-by-id item-id) :todo_list_id)]
    (data/delete-item-by-id (current-user-id) item-id)
    (redirect-to-list list-id)))

(defn restore-item [ item-id ]
  (let [ list-id ((data/get-item-by-id item-id) :todo_list_id)]
    (data/restore-item item-id)
    (redirect-to-list list-id)))

(defn selected-user-ids-from-params [ params ]
  (map #(Integer/parseInt (.substring % 5))
       (filter #(.startsWith % "user_") (map name (keys params)))))

(defroutes all-routes
  (GET "/" [] (redirect-to-home))

  (POST "/list" { { list-description :list-description } :params }
        (add-list (string-leftmost list-description 32)))

  (GET "/list/:list-id" { { list-id :list-id completed-within-days :cwithin } :params } 
       (ensure-list-access list-id)
       (render-todo-list-page list-id (or (parsable-integer? completed-within-days)
                                          0)))

  (GET "/list/:list-id/details" [ list-id ]
       (ensure-list-access list-id)
       (render-todo-list-details-page list-id))

  (POST "/list/:list-id/details" { params :params }
        (let [ { list-id :list-id 
                list-name :list-name
                share-with-email :share-with-email }
               params ]
          (ensure-list-access list-id)
          (update-list-description list-id (string-leftmost list-name 32))
          (add-list-owner list-id share-with-email
                          (selected-user-ids-from-params params))))


  (POST "/list/:list-id/delete" { { list-id :list-id  } :params }
        (ensure-list-access list-id)
        (delete-list list-id))

  (POST "/list/:list-id" { { list-id :list-id
                            item-description :item-description }
                           :params }
        (ensure-list-access list-id)
        (add-item list-id (string-leftmost item-description 1024)))

  (POST "/item/:item-id"  { { item-id :item-id description :description} :params}
        (ensure-item-access item-id)
        (update-item-desc item-id (string-leftmost description 1024)))
  
  (POST "/item-list" { { target-item :target-item target-list :target-list}
                       :params}
        (ensure-item-access target-item)
        (ensure-list-access target-list)
        (update-item-list target-item target-list))

  (POST "/item/:item-id/priority" { { item-id :item-id
                                     new-priority :new-priority}
                                    :params }
        (ensure-item-access item-id)
        (update-item-priority item-id new-priority))
  
  (POST "/item/:item-id/complete" [ item-id ]
        (ensure-item-access item-id)
        (complete-item item-id))

  (POST "/item/:item-id/delete" [ item-id ]
        (ensure-item-access item-id)
        (delete-item item-id))

  (POST "/item/:item-id/restore" [ item-id ]
        (ensure-item-access item-id)
        (restore-item item-id)))
