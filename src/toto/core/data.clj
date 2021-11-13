(ns toto.core.data
  (:use toto.core.util
        sql-file.sql-util)
  (:require [clojure.tools.logging :as log]
            [clojure.java.jdbc :as jdbc]
            [sql-file.core :as sql-file]))

(def ^:dynamic *db* nil)

(defn call-with-db-connection [ fn db-connection ]
  (jdbc/with-db-connection [ conn db-connection ]
    (binding [ *db* conn ]
      (fn))))

(defmacro with-db-connection [ db-connection & body ]
  `(call-with-db-connection (fn [] ~@body) ~db-connection))

(defn current-db-connection []
  (when (not *db*)
    (throw (RuntimeException. "No current database connection for query.")))
  *db*)

(defn wrap-db-connection [ app db-connection ]
  (fn [ req ]
    (call-with-db-connection (fn [] (app req)) db-connection)))

(defn scalar
  ([ query-result default ]
   (or
    (let [first-row (first query-result)]
      (get first-row (first (keys first-row))))
    default))

  ([ query-result ]
   (scalar query-result nil)))

(defn db-conn-spec [ config ]
  (let [ spec {:name (or (config-property "db.subname")
                         (get-in config [:db :subname] "toto"))
               :schema-path [ "sql/" ]
               :schemas [[ "toto" 9 ]]}]
    (log/info "DB Conn Spec: " spec)
    spec))

;;; backup

(def date-format (java.text.SimpleDateFormat. "yyyyMMdd-hhmm"))

(defn- get-backup-filename [ backup-path ]
  (str
   backup-path
   "/toto-backup-"
   (.format date-format (java.util.Date.))
   ".tgz"))

(defn backup-database [ backup-path ]
  (let [ output-path (get-backup-filename backup-path)]
    (log/info "Backing database up to" output-path)
    (sql-file/backup-to-file-online (current-db-connection) output-path)
    (log/info "Database backup complete.")))
