;; Copyright (c) 2015-2023 Michael Schaeffer (dba East Coast Toolworks)
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

(ns base.session
  (:use playbook.core
        sql-file.sql-util
        sql-file.middleware)
  (:require [taoensso.timbre :as log]
            [ring.middleware.session.store :as store]
            [clojure.edn :as edn]
            [base.queries :as query])
  (:import [java.util UUID]))

(def access-date-stale-threshold
  (* 2 60 60 1000))

(defn- update-sql-session-access-date [ session-key ]
  (let [ accessed-on (current-time)]
    (query/set-session-accessed-on! {:accessed_on_day accessed-on
                                     :session_key session-key})
    accessed-on))

(defn- session-stale? [ session ]
  (> (- (.getTime (current-time))
        (.getTime (:accessed_on_day session)))
     access-date-stale-threshold))

(defn- get-sql-session [ session-key ]
  (when-let [ session (first (query/query-sql-session {:session_key session-key}))]
    (assoc (edn/read-string (:session_value session))
           :accessed_on_day (if (session-stale? session)
                              (update-sql-session-access-date session-key)
                              (:accessed_on_day session)))))

(defn- write-sql-session [ session-key value ]
  (let [ session-map {:session_key session-key
                      :session_value (str value)
                      :updated_on (current-time)
                      :accessed_on_day (current-time)}]
    (if (get-sql-session session-key)
      (query/update-session! session-map)
      (query/create-session! session-map))))

(deftype SQLStore [ ]
  store/SessionStore

  (read-session [_ session-key]
    (get-sql-session session-key))

  (write-session [_ session-key value]
    (let [session-key (or session-key (str (UUID/randomUUID)))]
      (write-sql-session session-key value)
      session-key))

  (delete-session [_ session-key]
    (query/delete-session! {:session_key session-key})
    nil))

(deftype StoreMemoryCache [ underlying cache ]
  store/SessionStore

  (read-session [_ session-key]
    (let [ session (or (aand (@cache session-key)
                             (if (session-stale? it)
                               (.read-session underlying session-key)
                               it))
                       (.read-session underlying session-key))]
      (swap! cache assoc session-key session)
      session))

  (write-session [_ session-key value]
    (swap! cache dissoc session-key)
    (.write-session underlying session-key value))

  (delete-session [_ session-key]
    (swap! cache dissoc session-key)
    (.delete-session underlying session-key)))

(defn session-store [  ]
  (StoreMemoryCache. (SQLStore.) (atom {})))

(defn delete-old-web-sessions [ session-store ]
  (let [ stale-sessions (query/get-stale-sessions) ]
    (log/info (count stale-sessions) "stale web session(s) to be deleted.")
    (doseq [ session stale-sessions ]
      (.delete-session session-store (:session_key session)))))
