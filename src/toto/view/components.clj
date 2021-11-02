(ns toto.view.components
  (:use toto.view.query
        toto.view.icons)
  (:require [clojure.data.json :as json]
            [clojure.tools.logging :as log]))

(defn render-scroll-column [ title & contents ]
  [:div.scroll-column
   [:div.fixed title]
   [:div#scroll-list.scrollable  { :data-preserve-scroll "true" }
    contents]])

(defn post-button [ target args desc body ]
  ;; desc to be used for accessibility purposes
  [:span.clickable.post-button
   {:onclick (str "doPost('" target "'," (json/write-str args) ")")}
   body])

(defn post-button-shortcut [ target args desc shortcut-key body next-url]
  ;; desc to be used for accessibility purposes
  [:span.clickable.post-button
   {:data-shortcut-key shortcut-key
    :data-target target
    :onclick (str "doPost('" target "'," (json/write-str args) ", '" next-url "')")}
   body])

(defn post-button* [ target args desc body next-url ]
  ;; desc to be used for accessibility purposes
  [:span.clickable.post-button
   {:onclick (str "doPost('" target "'," (json/write-str args) ", '" next-url "')")}
   body])

(defn item-priority-button [ item-id new-priority image-spec writable? ]
  (if writable?
    (post-button (str "/item/" item-id "/priority") {:new-priority new-priority}
                 "Set Priority" image-spec)
    image-spec))

(defn render-item-priority-control [ item-id priority writable? ]
  (if (<= priority 0)
    (item-priority-button item-id 1 img-star-gray writable?)
    (item-priority-button item-id 0 img-star-yellow writable?)))

(defn list-priority-button [ list-id new-priority image-spec ]
  (post-button (shref "/list/" list-id "/priority") {:new-priority new-priority}
               "Set Priority" image-spec))

(defn render-list-star-control [ list-id priority ]
  (if (<= priority 0)
    (list-priority-button list-id 1 img-star-gray)
    (list-priority-button list-id 0 img-star-yellow)))

(defn render-list-arrow-control [ list-id priority ]
  (if (>= priority 0)
    (list-priority-button list-id -1 img-arrow-gray)
    (list-priority-button list-id 0 img-arrow-blue)))

