;; Copyright (c) 2015-2022 Michael Schaeffer (dba East Coast Toolworks)
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


(ns toto.view.query)

 ;;; Persistent Query

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
