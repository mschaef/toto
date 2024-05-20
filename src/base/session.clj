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
            [clojure.java.jdbc :as jdbc]
            [clojure.edn :as edn])
  (:import [java.util UUID]))

(def access-date-stale-threshold
  (* 2 60 60 1000))

(defn- update-sql-session-access-date [ db-conn session-key ]
  (log/debug :update-sql-session-access-date session-key)
  (let [ accessed-on (current-time)]
    (jdbc/update! db-conn
                  :web_session
                  {:accessed_on_day accessed-on}
                  ["session_key=?" session-key])
    accessed-on))

(defn- query-sql-session [ db-conn session-key ]
  (query-first db-conn
               [(str "SELECT session_value, accessed_on_day"
                     "  FROM web_session"
                     " WHERE session_key = ?")
                session-key]))

(defn- session-stale? [ session ]
  (> (- (.getTime (current-time))
        (.getTime (:accessed_on_day session)))
     access-date-stale-threshold))

(defn- get-sql-session [ db-conn session-key ]
  (log/debug :get-sql-session session-key)
  (when-let [ session (query-sql-session db-conn session-key)]
    (assoc (edn/read-string (:session_value session))
           :accessed_on_day (if (session-stale? session)
                              (update-sql-session-access-date db-conn session-key)
                              (:accessed_on_day session)))))

(defn- write-sql-session [ db-conn session-key value ]
  (log/debug :write-sql-session session-key value)
  (if (get-sql-session db-conn session-key)
    (jdbc/update! db-conn
                  :web_session
                  {:session_value (str value)
                   :updated_on (current-time)
                   :accessed_on_day (current-time)}
                  ["session_key=?" session-key])
    (jdbc/insert! db-conn
                  :web_session
                  {:session_key session-key
                   :session_value (str value)
                   :updated_on (current-time)
                   :accessed_on_day (current-time)})))

(defn- delete-sql-session [db-conn session-key]
  (log/debug :delete-sql-session session-key)
  (jdbc/delete! (current-db-connection)
                :web_session ["session_key=?" session-key]))

(deftype SQLStore [ db-conn ]
  store/SessionStore

  (read-session [_ session-key]
    (get-sql-session db-conn session-key))

  (write-session [_ session-key value]
    (let [session-key (or session-key (str (UUID/randomUUID)))]
      (write-sql-session db-conn session-key value)
      session-key))

  (delete-session [_ session-key]
    (delete-sql-session db-conn session-key)
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

(defn session-store [ db-conn ]
  (StoreMemoryCache. (SQLStore. db-conn) (atom {})))

(defn- get-stale-sessions []
  (query-all (current-db-connection)
             [(str "SELECT session_key"
                   "  FROM web_session"
                   " WHERE accessed_on_day < DATEADD('month', -1, CURRENT_TIMESTAMP)")]))

(defn delete-old-web-sessions [ session-store ]
  (let [ stale-sessions (get-stale-sessions) ]
    (log/info (count stale-sessions) "stale web session(s) to be deleted.")
    (doseq [ session stale-sessions ]
      (.delete-session session-store (:session_key session)))))
