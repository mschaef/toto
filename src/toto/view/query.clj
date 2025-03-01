;; Copyright (c) 2015-2025 Michael Schaeffer (dba East Coast Toolworks)
;;
;; Licensed as below.
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;       http://www.apache.org/licenses/LICENSE-2.0
;;
;; The license is also includes at the root of the project in the file
;; LICENSE.
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.
;;
;; You must not remove this notice, or any other, from this software.

(ns toto.view.query
  (:require [taoensso.timbre :as log]))

;;; Persistent Query

(def ^:dynamic *query* nil)
(def ^:dynamic *current-uri* nil)
(def ^:dynamic *current-modal* nil)

(defn wrap-remember-query[ app ]
  (fn [ req ]
    (binding [*query* (:query-params req)
              *current-uri* (:uri req)
              *current-modal* (:modal (:params req))]
      (app req))))

(defn- normalize-param-map [ params ]
  (into {} (map (fn [[ param val]] [ (keyword param) val ])
                params)))

(defn- merge-param-maps [ param-maps ]
  (into {} (remove (fn [[ _ val]] (= val :remove))
                (apply merge (map normalize-param-map param-maps)))))

(defn- shref* [ & args ]
  (let [url-param (apply str (remove map? args))
        url (if (> (count url-param) 0)
              url-param
              *current-uri*)
        query-params (merge-param-maps (filter map? args))]
    (let [query-string (clojure.string/join "&" (map (fn [[ param val ]] (str (name param) "=" val)) query-params))]
      (if (> (.length query-string) 0)
        (str url "?" query-string)
        url))))

(defn shref [ & args ]
  (apply shref* (or *query* {}) args))

(defn current-modal []
  *current-modal*)
