(ns toto.list-view
  (:use toto.util
        toto.view-utils)
  (:require [clojure.tools.logging :as log]
            [hiccup.form :as form]
            [toto.data :as data]
            [toto.view :as view]
            [toto.user :as user]
            [toto.sidebar-view :as sidebar-view]))

(defn- complete-item-button [ item-info ]
  (post-button (str "/item/" (item-info :item_id) "/complete") {} "Complete Item" img-check))

(defn- restore-item-button [ item-info ]
  (post-button (str "/item/" (item-info :item_id) "/restore") {}  "Restore Item" img-restore))

(defn- snooze-item-button [ item-info body ]
  (post-button (str "/item/" (item-info :item_id) "/snooze") {:snooze-days 1} "Snooze Item 1 Day" body))

(defn- unsnooze-item-button [ item-info body ]
  (post-button (str "/item/" (item-info :item_id) "/snooze") {:snooze-days 0} "Un-snooze Item" body))

(defn- render-new-item-form [ list-id ]
  (form/form-to
   {:class "new-item-form"}
   [:post (shref "/list/" list-id)]
   (form/text-field {:class "simple-border"
                     :maxlength "1024"
                     :placeholder "New Item Description"
                     :autofocus "autofocus"
                     :autocomplete "off"
                     :onkeydown "onNewItemInputKeydown(event)"}
                    "item-description")
   (form/hidden-field "item-priority" "0")
   [:button.high-priority-submit {:type "button"
                                  :onclick "submitHighPriority()"}
    img-star-yellow]))

(def url-regex #"(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")

(defn- render-url [ [ url ] ]
  [:a { :href url :target "_blank" } (shorten-url-text url 60)])

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

(defn- render-todo-item [ item-info writable? ]
  (let [{item-id :item_id
         is-complete? :is_complete
         is-deleted? :is_deleted
         priority :priority
         snoozed-until :snoozed_until
         currently-snoozed :currently_snoozed
         created-by-id :created_by_id
         created-by-name :created_by_name}
        item-info]
    [:div.item-row.order-drop-target.todo-item
     {:id (str "item_row_" item-id)
      :itemid item-id
      :ordinal (:item_ordinal item-info)
      :priority priority
      :class (class-set {"high-priority" (> priority 0)
                         "snoozed" currently-snoozed})}
     (item-drag-handle "left" item-info)
     (when writable?
       [:div.item-control.complete {:id (str "item_control_" item-id)}
        (if (or is-complete? is-deleted?)
          (restore-item-button item-info)
          (complete-item-button item-info))])
     [:div.item-control.priority.left
      (render-item-priority-control item-id priority writable?)]
     [:div.item-description {:itemid item-id}
      (let [desc (item-info :desc)]
        (list
         [:div { :id (str "item_desc_" item-id) :class "hidden"}
          (hiccup.util/escape-html desc)]
         [:div {:id (str "item_" item-id)
                :class (class-set {"deleted-item" is-deleted?
                                   "completed-item" is-complete?})}
          (render-item-text desc)
          ;;(:item_ordinal item-info)
          (snooze-item-button item-info [:span.pill (render-age (:age_in_days item-info))])
          (when currently-snoozed
            (unsnooze-item-button item-info [:span.pill "snoozed"]))
          (when (not (= created-by-id (current-user-id)))
            [:span.pill created-by-name])]))]
     [:div.item-control.priority.right
      (render-item-priority-control item-id priority writable?)]
     (item-drag-handle "right" item-info)]))


(defn- render-todo-list-query-settings [ list-id completed-within-days include-snoozed? ]
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
                   "Include items completed within: "]
                  [:select { :id "cwithin" :name "cwithin" :onchange "this.form.submit()"}
                   (form/select-options [ [ "-" "-"] [ "1d" "1"] [ "7d" "7"] [ "30d" "30"] [ "90d" "90"] ]
                                        (if (nil? completed-within-days)
                                          "-"
                                          (str completed-within-days)))]]
                 [:div.control-segment
                  [:label {:for "include-snoozed"}  "Include snoozed :"]
                  [:input {:type "checkbox"
                           :name "include-snoozed"
                           :id "include-snoozed"
                           :value "Y"
                           :onchange "this.form.submit()"
                           :checked include-snoozed?}]])])

(defn- render-empty-list []
  [:div.empty-list
   [:h1
    "Nothing to do right now!"]
   [:p
    "To get started, you can add new items in the box above."]])


(defn- render-snoozed-item-warning [ n-snoozed-items ]
  [:div.snoozed-item-warning
   n-snoozed-items " item" (if (= n-snoozed-items 1) "" "s" ) " snoozed for later. "
   "Click " [:a {:href (shref "" {:include-snoozed "Y"})} "here"] " to show."])

(defn- render-todo-list [ list-id writable? completed-within-days include-snoozed? ]
  (let [pending-items (data/get-pending-items list-id completed-within-days)
        n-snoozed-items (count (filter :currently_snoozed pending-items))]
    (render-scroll-column
     (when writable?
       (render-new-item-form list-id))
     [:div.toplevel-list
      (let [display-items (if include-snoozed?
                            pending-items
                            (remove :currently_snoozed pending-items))]
        (if (= (count display-items) 0)
          (render-empty-list)
          (list
           (map #(render-todo-item % writable?) display-items)
           (drop-target (+ 1 (apply max (map :item_ordinal display-items)))))))]
     (when (and (> n-snoozed-items 0)
                (not include-snoozed?))
       (render-snoozed-item-warning n-snoozed-items))
     (render-todo-list-query-settings list-id completed-within-days include-snoozed?))))

(defn render-todo-list-csv [  list-id ]
  (clojure.string/join "\n" (map :desc (data/get-pending-items list-id 0))))

(defn render-todo-list-page [ selected-list-id completed-within-days snoozed-for-days ]
  (view/render-page {:page-title ((data/get-todo-list-by-id selected-list-id) :desc)
                     :page-data-class "todo-list"
                     :sidebar (sidebar-view/render-sidebar-list-list selected-list-id snoozed-for-days)}
                    (render-todo-list selected-list-id true completed-within-days snoozed-for-days)))

(defn render-todo-list-public-page [ selected-list-id ]
  (view/render-page {:page-title ((data/get-todo-list-by-id selected-list-id) :desc)
                     :page-data-class "todo-list"}
                    (render-todo-list selected-list-id false 0 0)))

