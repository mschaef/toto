(ns toto.view-utils
  (:use [slingshot.slingshot :only (throw+ try+)])
  (:require [clojure.tools.logging :as log]
            [clojure.data.json :as json]
            [cemerick.friend :as friend]
            [hiccup.form :as form]))

(def ^:dynamic *dev-mode* false)

(defn wrap-dev-mode [ app dev-mode ]
  (fn [ req ]
    (binding [ *dev-mode* dev-mode ]
      (app req))))

(def ^:dynamic *query* nil)

(defn wrap-remember-query[ app ]
  (fn [ req ]
    (binding [ *query* (:query-params req) ]
      (app req))))

(defn call-with-modified-query [ mfn f ]
  (binding [ *query* (mfn *query*) ]
    (f)))

(defmacro with-modified-query [ mfn & body ]
  `(call-with-modified-query ~mfn (fn [] ~@body)))

(defn- normalize-param-map [ params ]
  (into {} (map (fn [[ param val]] [ (keyword param) val ])
                params)))

(defn shref* [ & args ]
  (let [url (apply str (remove map? args))
        query-params (apply merge (map normalize-param-map (filter map? args)))]
    (let [query-string (clojure.string/join "&" (map (fn [[ param val ]] (str (name param) "=" val)) query-params))]
      (if (> (.length query-string) 0)
        (str url "?" query-string)
        url))))

(defn shref [ & args ]
  (apply shref* (or *query* {}) args))

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
   [:div#scroll-list.scrollable  { :data-preserve-scroll "true" }
    contents]])

(defn current-identity []
  (if-let [auth (friend/current-authentication)]
    (:identity auth)))

(defn current-user-id []
  (if-let [ cauth (friend/current-authentication) ]
    (:user-id cauth)))
