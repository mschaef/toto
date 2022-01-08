(ns toto.todo.todo-list
  (:use toto.core.util
        toto.view.common
        toto.view.icons
        toto.view.components
        toto.view.query
        toto.view.page)
  (:require [clojure.tools.logging :as log]
            [hiccup.form :as form]
            [hiccup.util :as util]
            [toto.data.data :as data]
            [toto.view.auth :as auth]
            [toto.todo.sidebar-view :as sidebar-view]))

(def html-breakpoint "&#8203;")

(defn ensure-string-breakpoints [ s n ]
  (clojure.string/join html-breakpoint (partition-string s n)))

(defn- ensure-string-breaks [ string at ]
  (clojure.string/replace string at (str at html-breakpoint)))

(defn shorten-url-text [ url-text target-length ]
  (let [url (java.net.URL. url-text)
        base (str (.getProtocol url)
                  ":"
                  (if-let [authority (.getAuthority url)]
                    (str "//" authority)))]
    (-> (util/escape-html
         (str base
              (string-leftmost (.getPath url)
                               (max 0 (- (- target-length 3) (.length base)))
                               "...")))
        (ensure-string-breaks "/")
        (ensure-string-breaks "."))))

(defmacro without-edit-id [ & body ]
  `(with-modified-query #(dissoc % "edit-item-id")
     ~@body))

(defmacro without-modal [ & body ]
  `(with-modified-query #(-> %
                             (dissoc "snoozing")
                             (dissoc "updating-from"))
     ~@body))

(defn- complete-item-button [ item-info ]
  (post-button {:desc "Complete Item"
                :target (str "/item/" (item-info :item_id) "/complete")}
               img-check))

(defn- restore-item-button [ item-info ]
  (post-button {:desc "Restore Item"
                :target (str "/item/" (item-info :item_id) "/restore")}
               img-restore))

(defn- delete-item-button [ item-info list-id ]
  (post-button {:desc "Delete Item"
                :target (str "/item/" (item-info :item_id) "/delete")
                :next-url (without-edit-id (shref "/list/" list-id))}
               img-trash))

(defn- snooze-item-button [ item-info body ]
  [:a {:href (shref "" {:snoozing (item-info :item_id)})} body])

(defn- item-priority-button [ item-id new-priority image-spec writable? ]
  (if writable?
    (post-button {:target (str "/item/" item-id "/priority")
                  :args {:new-priority new-priority}
                  :desc "Set Item Priority"}
                 image-spec)
    image-spec))

(defn- render-item-priority-control [ item-id priority writable? ]
  (if (<= priority 0)
    (item-priority-button item-id 1 img-star-gray writable?)
    (item-priority-button item-id 0 img-star-yellow writable?)))

(defn- render-new-item-form [ list-id editing-item? ]
  (form/form-to
   {:class "new-item-form"}
   [:post (shref "/list/" list-id)]
   (form/text-field (cond-> {:class "simple-border"
                             :maxlength "1024"
                             :placeholder "New Item Description"
                             :autocomplete "off"
                             :onkeydown "onNewItemInputKeydown(event)"}
                      (not editing-item?) (assoc "autofocus" "on"))
                    "item-description")
   (form/hidden-field "item-priority" "0")
   [:button.high-priority-submit {:type "button"
                                  :onclick "submitHighPriority()"}
    img-star-yellow]))

(def url-regex #"(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")

(defn- render-url [ [ url ] ]
  [:a.item-link { :href url :target "_blank" } (shorten-url-text url 60)])

(defn- render-item-text-segment [ item-text-segment ]
  (clojure.string/join " " (map #(ensure-string-breakpoints % 15)
                                (clojure.string/split item-text-segment #"\s"))))

(defn- render-item-text [ item-text ]
  (interleave (conj (vec (map #(str " " (render-item-text-segment (.trim %)) " ") (clojure.string/split item-text url-regex))) "")
              (conj (vec (map render-url (re-seq url-regex item-text))) "")))

(defn- render-age [ days ]
  (cond (> days 720) (str (quot days 360) "y")
        (> days 60) (str (quot days 30) "m")
        :else (str days "d")))

(defn item-drag-handle [ class item-info ]
  [:div.item-control.drag-handle {:itemid (:item_id item-info)
                                  :class class}
   img-bars])

(defn drop-target [ item-ordinal ]
  [:div.order-drop-target {:ordinal item-ordinal :priority "0"} "&nbsp;"])

(def snooze-date-format (java.text.SimpleDateFormat. "yyyy-MM-dd"))

(defn- render-todo-item [ list-id item-info writable? editing? ]
  (let [{item-id :item_id
         is-complete? :is_complete
         is-deleted? :is_deleted
         priority :priority
         snoozed-until :snoozed_until
         currently-snoozed :currently_snoozed
         created-by-id :created_by_id
         created-by-name :created_by_name}
        item-info]
    [:div.item-row.order-drop-target
     (cond-> {:id (str "item_row_" item-id)
              :itemid item-id
              :ordinal (:item_ordinal item-info)
              :priority priority
              :class (class-set {"editing" editing?
                                 "display" (not editing?) 
                                 "high-priority" (> priority 0)
                                 "snoozed" currently-snoozed})}
       writable? (assoc :edit-href (shref "/list/" list-id { :edit-item-id item-id })))
     (if editing?
       (list
        [:div.item-control.complete
         (delete-item-button item-info list-id)]
         [:div.item-description
          [:input (cond-> {:value (item-info :desc)
                           :type "text"
                           :name "description"
                           :item-id item-id
                           :view-href (without-edit-id (shref "/list/" list-id))
                           :onkeydown "onItemEditKeydown(event)"}
                    editing? (assoc "autofocus" "on"))]])
       (list
        (when writable?
          (list
           (item-drag-handle "left" item-info)
           [:div.item-control.complete {:id (str "item_control_" item-id)}
            (if (or is-complete? is-deleted?)
              (restore-item-button item-info)
              (complete-item-button item-info))]))
        [:div.item-control.priority.left
         (render-item-priority-control item-id priority writable?)]
        [:div.item-description {:itemid item-id}
         (let [desc (item-info :desc)]
           (list
            [:div {:id (str "item_" item-id)
                   :class (class-set {"deleted-item" is-deleted?
                                      "completed-item" is-complete?})}
             (render-item-text desc)
             (snooze-item-button item-info [:span.pill
                                            (render-age (:age_in_days item-info))
                                            (when currently-snoozed
                                              (list
                                               ", snoozed: " (.format snooze-date-format snoozed-until)))])
             (when (not (= created-by-id (auth/current-user-id)))
               [:span.pill created-by-name])]))]))
     [:div.item-control.priority.right
      (render-item-priority-control item-id priority writable?)]
     (item-drag-handle "right" item-info)]))


(defn- render-query-select [ id current-value allow-all? ]
  [:select { :id id :name id :onchange "this.form.submit()"}
   (form/select-options (cond-> [ [ "-" "-"] ["1d" "1"] ["7d" "7"] ["30d" "30"] ["90d" "90"] ]
                          allow-all? (conj ["*" "99999"] ))
                        (if (nil? current-value)
                          "-"
                          (str current-value)))])

(defn- render-todo-list-query-settings [ list-id completed-within-days snoozed-for-days ]
  [:div.query-settings
   (form/form-to { :class "embedded "} [:get (shref "/list/" list-id)]
                 [:div.control-segment
                  [:a {:href (shref "/list/" list-id "/details")}
                   "[list details]"]]
                 [:div.control-segment
                  [:a {:href (str list-id)}
                   " [default view]"]]
                 [:div.control-segment
                  [:label {:for "cwithin"}
                   "Completed within: "]
                  (render-query-select "cwithin" completed-within-days false)
                  [:label {:for "swithin"}
                   "Snoozed for: "]
                  (render-query-select "sfor" snoozed-for-days true)]
                 [:div.control-segment
                  [:a { :href (shref "/list/" list-id {:updating-from "Y"} ) } "[copy from]"]]
                 [:div.control-segment
                  [:a { :href (shref "/list/" list-id "/list.csv" ) } "[csv]"]])])

(defn- render-empty-list []
  [:div.empty-list
   [:h1
    "Nothing to do right now!"]
   [:p
    "To get started, you can add new items in the box above."]])

(defn- render-snoozed-item-warning [ n-snoozed-items ]
  [:div.snoozed-item-warning
   n-snoozed-items " more item" (if (= n-snoozed-items 1) "" "s" ) " snoozed for later. "
   "Click " [:a {:href (shref "" {:sfor "99999"})} "here"] " to show."])

(defn- render-todo-list [ list-id edit-item-id writable? completed-within-days snoozed-for-days ]
  (let [pending-items (data/get-pending-items list-id completed-within-days snoozed-for-days)
        n-snoozed-items (count (filter :visibly_snoozed pending-items))]
    (scroll-column
     "todo-list-scroller"
     (when writable?
       (render-new-item-form list-id (boolean edit-item-id)))
     [:div.toplevel-list
      (let [display-items (remove :visibly_snoozed pending-items)]
        (if (= (count display-items) 0)
          (render-empty-list)
          (list
           (map #(render-todo-item list-id % writable? (= edit-item-id (:item_id %)))
                display-items)
           (drop-target (+ 1 (apply max (map :item_ordinal display-items)))))))]
     (when (> n-snoozed-items 0)
       (render-snoozed-item-warning n-snoozed-items))
     (render-todo-list-query-settings list-id completed-within-days snoozed-for-days))))

(defn render-todo-list-csv [  list-id ]
  (clojure.string/join "\n" (map :desc (data/get-pending-items list-id 0 0))))

(defn render-snooze-modal [list-id snoozing-item-id]
  (let [currently-snoozed (:currently_snoozed (data/get-item-by-id snoozing-item-id))
        list-url (without-modal (shref "/list/" list-id))]

    (defn render-snooze-choice [ label snooze-days shortcut-key ]
      (post-button {:desc (str label " (" shortcut-key ")")
                    :target (str "/item/" snoozing-item-id "/snooze")
                    :args {:snooze-days snooze-days}
                    :shortcut-key shortcut-key
                    :next-url list-url}
                   (str label " (" shortcut-key ")")))

    (render-modal
     list-url
     [:div.snooze
      [:h3 "Snooze item until later"]
      [:div.choices
       (map (fn [ [ label snooze-days shortcut-key] ]
              (render-snooze-choice label snooze-days shortcut-key))
            [["Tomorrow" 1 "1"]
             ["Next Week"  7 "2"]
             ["Next Month" 30 "3"]
             ["Next Year" 365 "4"]])]
      (when currently-snoozed
        [:div.choices
         [:hr]
         (render-snooze-choice "Unsnooze" 0 "0")])])))

(defn- render-list-select [ id excluded-list-id ]
  [:select { :id id :name id }
   (form/select-options (map (fn [ list-info ]
                               [ (:desc list-info) (:todo_list_id list-info)])
                             (remove
                              #(= excluded-list-id (:todo_list_id %))
                              (data/get-todo-lists-by-user (auth/current-user-id)))))])

(defn render-update-from-modal [ list-id ]
  (let [ list-url (without-modal (shref "/list/" list-id))]
    (render-modal
     list-url
     [:h3 "Update From"]
     (form/form-to
      [:post (without-modal (shref "/list/" list-id "/copy-from"))]
      "Source:"
      (render-list-select "copy-from-list-id" (parsable-integer? list-id))
      [:div.modal-controls
       [:input {:type "submit" :value "Copy List"}]]))))

(defn render-todo-list-page [ selected-list-id edit-item-id min-list-priority completed-within-days snoozed-for-days snoozing-item-id updating ]
  (render-page {:page-title ((data/get-todo-list-by-id selected-list-id) :desc)
                :page-data-class "todo-list"
                :sidebar (sidebar-view/render-sidebar-list-list selected-list-id min-list-priority snoozed-for-days)}
               (when snoozing-item-id
                 (render-snooze-modal selected-list-id snoozing-item-id))
               (when updating
                 (render-update-from-modal selected-list-id))
               (render-todo-list selected-list-id edit-item-id true completed-within-days snoozed-for-days)))

(defn render-todo-list-public-page [ selected-list-id ]
  (render-page {:page-title ((data/get-todo-list-by-id selected-list-id) :desc)
                :page-data-class "todo-list"}
               (render-todo-list selected-list-id nil false 0 0)))

