(ns toto.view-utils
  (:use [slingshot.slingshot :only (throw+ try+)])
  (:require [cemerick.friend :as friend]
            [hiccup.form :as form]))

(def img-group [:i {:class "fa fa-group icon-gray"}])

(def img-star-gray [:i {:class "fa fa-lg fa-star-o icon-gray"}])
(def img-star-yellow [:i {:class "fa fa-lg fa-star icon-yellow"}])

(def img-arrow-gray [:i {:class "fa fa-lg fa-arrow-down icon-gray"}])
(def img-arrow-blue [:i {:class "fa fa-lg fa-arrow-down icon-blue"}])

(def img-edit-list [:i {:class "fa fa-pencil icon-edit"}])

(def img-check [:i {:class "fa fa-check icon-black"}])
(def img-trash [:i {:class "fa fa-trash-o icon-black"}])
(def img-restore [:i {:class "fa fa-repeat icon-black"}])

(defmacro catch-validation-errors [ & body ]
  `(try+
    ~@body
    (catch [ :type :form-error ] details#
      ( :markup details#))))

(defn fail-validation [ markup ]
  (throw+ { :type :form-error :markup markup }))

(defn report-unauthorized []
  (friend/throw-unauthorized (friend/current-authentication) {}))

(defn render-scroll-column [ title & contents ]
  [:div.scroll-column
   [:div.fixed title]
   [:div.scrollable contents]])

(defn post-button [ target desc body ]
  (form/form-to { :class "embedded" } [:post target]
                [:button.item-button {:type "submit" :value desc :title desc} body]))

(defn item-priority-button [ item-id new-priority image-spec writable? ]
  (if writable?
    (post-button (str "/item/" item-id "/priority?new-priority=" new-priority) "Set Priority" image-spec)
    image-spec))

(defn render-item-priority-control [ item-id priority writable? ]
  (if (<= priority 0)
    (item-priority-button item-id 1 img-star-gray writable?)
    (item-priority-button item-id 0 img-star-yellow writable?)))

(defn list-priority-button [ list-id new-priority image-spec ]
  (post-button (str "/list/" list-id "/priority?new-priority=" new-priority)
               "Set Priority" image-spec))

(defn render-list-star-control [ list-id priority ]
  (if (<= priority 0)
    (list-priority-button list-id 1 img-star-gray)
    (list-priority-button list-id 0 img-star-yellow)))

(defn render-list-arrow-control [ list-id priority ]
  (if (>= priority 0)
    (list-priority-button list-id -1 img-arrow-gray)
    (list-priority-button list-id 0 img-arrow-blue)))

