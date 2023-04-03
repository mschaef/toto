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

(ns toto.core.session
  (:use toto.core.util
        sql-file.sql-util
        sql-file.middleware)
  (:require [clojure.tools.logging :as log]
            [ring.middleware.session.store :as store]
            [clojure.java.jdbc :as jdbc]
            [clojure.edn :as edn])
  (:import [java.util UUID]))

(defn- get-session-value [ db-conn session-key ]
  (if-let [ value-text (query-scalar (current-db-connection)
                               [(str "SELECT session_value"
                                     "  FROM web_session"
                                     " WHERE session_key = ?")
                                session-key])]
    (edn/read-string value-text)))

(defn write-session-value [ db-conn key value ]
  (if (get-session-value db-conn key)
    (jdbc/update! db-conn
                  :web_session
                  {:session_value (str value)
                   :updated_on (current-time)
                   :accessed_on_day (current-time)}
                  ["session_key=?" key])

    (jdbc/insert! db-conn
                  :web_session
                  {:session_key key
                   :session_value (str value)
                   :updated_on (current-time)
                   :accessed_on_day (current-time)})))

(defn- delete-session-value [db-conn session-key]
  (jdbc/delete! (current-db-connection)
                :web_session ["session_key=?" session-key]))

(deftype SQLStore [ db-conn cache ]
  store/SessionStore

  (read-session [_ key]
    (log/debug :read-session key)
    (or (@cache key)
        (let [db-value (get-session-value db-conn key)]
          (swap! cache assoc key db-value)
          db-value)))

  (write-session [_ key value]
    (log/debug :write-session key value)
    (let [key (or key (str (UUID/randomUUID)))]
      (swap! cache assoc key value)
      (write-session-value db-conn key value)
      key))

  (delete-session [_ key]
    (log/debug :delete-session key)
    (swap! cache dissoc key)
    (delete-session-value db-conn key)
    nil))

(defn session-store [ db-conn ]
  (SQLStore. db-conn (atom {})))

