(ns toto.view-utils
  (:use [slingshot.slingshot :only (throw+ try+)])
  (:require [clojure.tools.logging :as log]
            [cemerick.friend :as friend]
            [hiccup.form :as form]))

(def ^:dynamic *query* nil)

(defn wrap-remember-query[ app ]
  (fn [ req ]
    (binding [ *query* (:query-params req) ]
      (app req))))

(defn shref* [ & args ]
  (let [url (apply str (remove map? args))]
    (let [query-string (clojure.string/join "&" (map (fn [[ param val ]] (str (name param) "=" val)) (apply merge (filter map? args))))]
      (if (> (.length query-string) 0)
        (str url "?" query-string)
        url))))

(defn shref [ & args ]
  (apply shref* (or *query* {}) args))


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

(defn post-button [ target args desc body ]
  (form/form-to { :class "embedded" } [:post target]
                (map (fn [[key val]]
                       [:input {:type "hidden" :name key :value val}])
                     args)
                [:button.item-button {:type "submit" :value desc :title desc} body]))

(defn item-priority-button [ item-id new-priority image-spec writable? ]
  (if writable?
    (post-button (shref "/item/" item-id "/priority") {:new-priority new-priority}
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

(defn current-identity []
  (if-let [auth (friend/current-authentication)]
    (:identity auth)))

(defn current-user-id []
  (if-let [ cauth (friend/current-authentication) ]
    (:user-id cauth)))
