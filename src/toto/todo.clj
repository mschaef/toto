(ns toto.todo
  (:use toto.util
        compojure.core
        [slingshot.slingshot :only (throw+ try+)])
  (:require [clojure.tools.logging :as log]
            [ring.util.response :as ring]
            [hiccup.form :as form]
            [hiccup.page :as page]
            [compojure.handler :as handler]
            [ring.util.response :as ring-response]
            [cemerick.friend :as friend]
            [toto.data :as data]
            [toto.core :as core]
            [toto.view :as view]
            [toto.user :as user]))

(defn- report-unauthorized []
  (friend/throw-unauthorized (friend/current-authentication) {}))

(defn current-todo-list-id []
  (friend/authorize #{:toto.role/verified}
                    (first (data/get-todo-list-ids-by-user (user/current-user-id)))))

(defn ensure-list-owner-access [ list-id ]
  (friend/authorize #{:toto.role/verified}
                    (unless (data/list-owned-by-user-id? list-id (user/current-user-id))
                            (report-unauthorized))))

(defn ensure-list-public-access [ list-id ]
  (unless (data/list-public? list-id)
          (report-unauthorized)))

(defn ensure-item-access [ item-id ]
  (friend/authorize #{:toto.role/verified}
                    (unless (data/item-owned-by-user-id? item-id (user/current-user-id))
                            (report-unauthorized))))

(defn redirect-to-list [ list-id ]
  (ring/redirect (str "/list/" list-id)))

(defn redirect-to-home []
  (redirect-to-list (current-todo-list-id)))

(def img-star-gray [:i {:class "fa fa-lg fa-star-o icon-gray"}])
(def img-star-yellow [:i {:class "fa fa-lg fa-star icon-yellow"}])

(def img-edit-list [:i {:class "fa fa-pencil icon-edit"}])

(def img-check [:i {:class "fa fa-check icon-black"}])
(def img-trash [:i {:class "fa fa-trash-o icon-black"}])
(def img-restore [:i {:class "fa fa-repeat icon-black"}])

(defn js-link [ js-fn-name args & contents ]
  [:a {:href (str "javascript:" js-fn-name "(" (clojure.string/join "," args) ")")}
   contents])

(defn post-button [ target desc body ]
  (form/form-to { :class "embedded" } [:post target]
                [:button.item-button {:type "submit" :value desc :title desc} body]))

(defn complete-item-button [ item-info ]
  (post-button (str "/item/" (item-info :item_id) "/complete") "Complete Item" img-check))

(defn restore-item-button [ item-info ]
  (post-button (str "/item/" (item-info :item_id) "/restore") "Restore Item" img-restore))

(defn snooze-item-button [ item-info body ]
  (post-button (str "/item/" (item-info :item_id) "/snooze?snooze-days=1") "Snooze Item 1 Day" body))

(defn unsnooze-item-button [ item-info body ]
  (post-button (str "/item/" (item-info :item_id) "/snooze?snooze-days=0") "Un-snooze Item" body))

(defn item-priority-button [ item-id new-priority image-spec writable? ]
  (if writable?
    (post-button (str "/item/"item-id "/priority?new-priority=" new-priority) "Set Priority" image-spec)
    image-spec))

(defn render-new-item-form [ list-id ]
  (form/form-to [:post (str "/list/" list-id)]
                (form/text-field { :class "full-width simple-border" :maxlength "1024" } "item-description")))

(def url-regex #"(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]") 

(defn render-url [ [ url ] ]
  [:a { :href url :target "_blank" } (shorten-url-text url 60)])

(defn render-item-text-segment [ item-text-segment ]
  (clojure.string/join " " (map #(clojure.string/join "&#8203;" (partition-string 15 %))
                                (clojure.string/split item-text-segment #"\s"))))

(defn render-item-text [ item-text ]
  (interleave (conj (vec (map #(str " " (render-item-text-segment (.trim %)) " ") (clojure.string/split item-text url-regex))) "")
              (conj (vec (map render-url (re-seq url-regex item-text))) "")))

(defn render-age [ days ]
  (cond (> days 720) (str (quot days 360) "y")
        (> days 60) (str (quot days 30) "m")
        :else (str days "d")))

(defn render-item-priority-control [ item-id priority writable? ]
  (if (<= priority 0)
    (item-priority-button item-id 1 img-star-gray writable?)
    (item-priority-button item-id 0 img-star-yellow writable?)))

(defn render-todo-item [ item-info item-number writable? ]
  (let [{item-id :item_id
         item-desc :desc
         item-age :age_in_days
         completed-on :completed_on
         is-delete? :is_delete
         priority :priority
         snoozed-until :snoozed_until}
        item-info]

    [:tr.item-row  {:itemid item-id
                    :class (class-set {"first-row" (= item-number 0)
                                       "high-priority" (> priority 0)})}
     (when writable?
       [:td.item-control
        [:div { :id (str "item_control_" item-id)}
         (if (nil? completed-on)
           (complete-item-button item-info)
           (restore-item-button item-info))]])
     [:td.item-control.priority.left
      (render-item-priority-control item-id priority writable?)]
     [:td.item-description
      (let [desc (item-info :desc)]
        (list
         [:div { :id (str "item_desc_" item-id) :class "hidden"}
          (hiccup.util/escape-html desc)]
         [:div {:id (str "item_" item-id)
                :class (class-set {"deleted_item" (and (not (nil? completed-on)) is-delete?)
                                   "completed_item" (not (nil? completed-on))})}
          (render-item-text desc)
          (snooze-item-button item-info [:span.pill (render-age item-age)])
          (when (and snoozed-until
                     (.after snoozed-until (java.util.Date.)))
            (unsnooze-item-button item-info [:span.pill "snoozed"]))]))]
     [:td.item-control.priority.right
      (render-item-priority-control item-id priority writable?)]]))

(defn render-todo-list [ list-id writable? completed-within-days snoozed-for-days ]
  [:table.item-list
   (when writable?
     [:tr {:class "new-item"}
      [:td {:colspan "3" :class "new-item"}
       (render-new-item-form list-id)]])
   (map (fn [ item-info item-number ]
           (render-todo-item item-info item-number writable?))
        (data/get-pending-items list-id completed-within-days snoozed-for-days)
        (range))])

(defn render-todo-list-list [ selected-list-id ]
  [:table.list-list
   (map (fn [ { list-id :todo_list_id list-desc :desc list-item-count :item_count is-public :is_public } ]
          [:tr (if (= list-id (Integer. selected-list-id))
                 { :class "selected" :listid list-id }
                 { :listid list-id })
           [:td.item-control
            [:a {:href (str "/list/" list-id "/details")} img-edit-list]]
           [:td.item
            [:a {:href (str "/list/" list-id)}
             (hiccup.util/escape-html list-desc)]
            [:span.pill list-item-count] 
            (when is-public
              [:span.public-flag
               [:a { :href (str "/list/" list-id "/public") } "public"]])]])
        (data/get-todo-lists-by-user (user/current-user-id)))
   [:tr.add-list
    [:td { :colspan "2"}
     (js-link "beginListCreate" nil "Add Todo List...")]]])

(defn render-item-set-list-form []
    (form/form-to { :class "embedded" :id (str "item_set_list_form") } [:post "/item-list"]
                  (form/hidden-field "target-item")
                  (form/hidden-field "target-list")))

(defn render-todo-list-csv [  list-id ]
  (clojure.string/join "\n" (map :desc (data/get-pending-items list-id 0 0))))

(defn render-todo-list-page [ selected-list-id completed-within-days snoozed-for-days ]
  (view/render-page {:page-title ((data/get-todo-list-by-id selected-list-id) :desc)
                     :init-map { :page "todo-list" }
                     :sidebar (render-todo-list-list selected-list-id)}
                    (render-item-set-list-form)
                    (render-todo-list selected-list-id true completed-within-days snoozed-for-days)
                    [:div.query-settings
                     (form/form-to { :class "embedded "} [ :get (str "/list/" selected-list-id)]
                                   "Include items completed within "
                                   [:select { :id "cwithin" :name "cwithin" :onchange "this.form.submit()"} 
                                    (form/select-options [ [ "-" "-"] [ "1d" "1"] [ "7d" "7"] [ "30d" "30"] [ "90d" "90"] ]
                                                         (if (nil? completed-within-days)
                                                           "-"
                                                           (str completed-within-days)))]

                                   " and those snoozed for "
                                   [:select { :id "sfor" :name "sfor" :onchange "this.form.submit()"} 
                                    (form/select-options [ [ "-" "-"] [ "1d" "1"] ]
                                                         (if (nil? snoozed-for-days)
                                                           "-"
                                                           (str snoozed-for-days)))]
                                   ".")]))

(defn render-todo-list-public-page [ selected-list-id ]
  (view/render-page {:page-title ((data/get-todo-list-by-id selected-list-id) :desc)
                     :init-map { :page "todo-list" }}
                    (render-todo-list selected-list-id false 0 0)))

(defn config-panel [ target-url & sections ]
  (form/form-to
   [:post target-url]
   [:table.config-panel
    (map (fn [ [ heading & body ] ]
           [:tr
            [:td.section-heading heading]
            [:td body ]])
         sections)]))

(defn render-todo-list-details-page [ list-id & { :keys [ error-message ]}]
  (let [list-details (data/get-todo-list-by-id list-id)
        list-name (:desc list-details)
        list-owners (data/get-todo-list-owners-by-list-id list-id) ]
    (view/render-page
     {:page-title (str "List Details: " list-name) 
      :sidebar (render-todo-list-list list-id) }
     (config-panel
      (str "/list/" list-id "/details")

      ["List Name:"
       (form/text-field { :class "full-width" :maxlength "32" } "list-name" list-name)]
      
      ["List Permissions:"
       (form/check-box "is_public" (:is_public list-details))
       [:label {:for "is_public"} "List publically visible?"]]
      
      ["List Owners:"
       [:table.item-list
        (map (fn [ { user-id :user_id user-email-addr :email_addr } ]
               (let [ user-parameter-name (str "user_" user-id)]
                 [:tr.item-row
                  [:td.item-control
                   (if (= (user/current-user-id) user-id)
                     (form/hidden-field user-parameter-name "on")
                     (form/check-box user-parameter-name
                                     (in? list-owners user-id)))]
                  [:td.item-description user-email-addr]]))
             (data/get-friendly-users-by-id (user/current-user-id)))
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
      ""
      [ "Download List:" [:a { :href (str "/list/" list-id "/list.csv" ) } "Download List as CSV"]])

     (config-panel
      (str "/list/" list-id "/delete")
      ["Delete List: " (if (data/empty-list? list-id)
                         (list 
                          [:input.dangerous {:type "submit" :value "Delete List"}]
                          [:span.warning "Warning, this cannot be undone."])
                         [:span.warning "To delete this list, remove all items first."])]))))

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
                                                   (hash-set email-user-id)))))))

(defn add-list [ list-description ]
  (if (string-empty? list-description)
    (redirect-to-home)
    (let [ list-id (data/add-list list-description) ]
      (data/set-list-ownership list-id #{ (user/current-user-id) })
      (redirect-to-list list-id))))

(defn add-item [ list-id item-description ]
  (when (not (string-empty? item-description))
    (data/add-todo-item list-id item-description))
  (redirect-to-list list-id))

(defn update-item-desc [ item-id item-description ]
  (let [ list-id (data/get-list-id-by-item-id item-id)]
    (when (not (string-empty? item-description))
      (data/update-item-desc-by-id item-id item-description))
    (redirect-to-list list-id)))

(defn update-item-snooze-days [ item-id snooze-days ]
  (let [list-id (data/get-list-id-by-item-id item-id)
        snooze-days (or (parsable-integer? snooze-days) 0)]
    (data/update-item-snooze-by-id item-id (if (= snooze-days 0)
                                             nil
                                             (add-days (java.util.Date.) snooze-days)))
    (redirect-to-list list-id)))

(defn update-item-list [ item-id target-list-id ] 
  (let [ original-list-id (data/get-list-id-by-item-id item-id)]
    (data/update-item-list item-id target-list-id)
    (redirect-to-list original-list-id)))

(defn update-item-priority [ item-id new-priority ]
  (let [ original-list-id (data/get-list-id-by-item-id item-id)]
    (data/update-item-priority-by-id item-id new-priority)
    (redirect-to-list original-list-id)))

(defn complete-item [ item-id ]
  (let [ list-id (data/get-list-id-by-item-id item-id)]
    (data/complete-item-by-id (user/current-user-id) item-id)
    (redirect-to-list list-id)))

(defn delete-item [ item-id ]
  (let [ list-id (data/get-list-id-by-item-id item-id)]
    (data/delete-item-by-id (user/current-user-id) item-id)
    (redirect-to-list list-id)))

(defn restore-item [ item-id ]
  (let [ list-id (data/get-list-id-by-item-id item-id)]
    (data/restore-item item-id)
    (redirect-to-list list-id)))

(defn selected-user-ids-from-params [ params ]
  (map #(Integer/parseInt (.substring % 5))
       (filter #(.startsWith % "user_") (map name (keys params)))))

(defn- public-routes [ config ]
  (routes
   (GET "/list/:list-id/public" { { list-id :list-id } :params }
     (log/error "public render " list-id)
     (ensure-list-public-access list-id)
     (render-todo-list-public-page list-id))))


(defn- list-routes [ list-id ]
  (ensure-list-owner-access list-id)
  (routes
   (GET "/" { params :params }
     (render-todo-list-page list-id
                            (or (parsable-integer? (:cwithin params)) 0)
                            (or (parsable-integer? (:sfor params)) 0)))
   
   (GET "/list.csv" []
     (-> (render-todo-list-csv list-id)
         (ring-response/response)
         (ring-response/header "Content-Type" "text/csv")))

   (GET "/details"  []
     (render-todo-list-details-page list-id))
    
   (POST "/details" { params :params }
     (let [ { list-name :list-name share-with-email :share-with-email is-public :is_public } params ]
       (update-list-description list-id (string-leftmost list-name 32))
       (add-list-owner list-id share-with-email (selected-user-ids-from-params params))
       (data/set-list-public list-id (boolean is-public))
       (ring/redirect  (str "/list/" list-id "/details"))))
    
   (POST "/delete" []
     (delete-list list-id))  
   
   (POST "/" { { item-description :item-description } :params }
     (add-item list-id (string-leftmost item-description 1024)))))

(defn- item-routes [ item-id ]
  (ensure-item-access item-id)
  (routes
   (POST "/"  { { description :description } :params }
     (update-item-desc item-id (string-leftmost description 1024)))
  
   (POST "/snooze" { { snooze-days :snooze-days } :params}
     (update-item-snooze-days item-id snooze-days))

   (POST "/priority" { { new-priority :new-priority } :params }
     (update-item-priority item-id new-priority))
  
   (POST "/complete" [ ]
     (complete-item item-id))

   (POST "/delete" [ ]
     (delete-item item-id))

   (POST "/restore" [ ]
     (restore-item item-id))))

(defn- private-routes [ config ]
  (routes
   (GET "/" [] (redirect-to-home))

   (POST "/list" { { list-description :list-description } :params }
     (add-list (string-leftmost list-description 32)))

   (context "/list/:list-id" [ list-id ]
     (list-routes list-id))
   
   (POST "/item-list" { { target-item :target-item target-list :target-list} :params}
     (ensure-item-access target-item)
     (ensure-list-owner-access target-list)
     (update-item-list target-item target-list))

   (context "/item/:item-id" [ item-id ]
     (item-routes item-id))))

(defn all-routes [ config ]
  (routes
   (public-routes config)
   (wrap-routes (private-routes config)
                friend/wrap-authorize
                #{:toto.role/verified})))
