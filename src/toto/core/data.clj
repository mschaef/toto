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
               :schemas [[ "toto" 8 ]]}]
    (log/info "DB Conn Spec: " spec)
    spec))

;;; backup

(defn backup-database [ output-path ]
  (log/info "Backing database up to" output-path)
  (sql-file/backup-to-file-online (current-db-connection) output-path)
  (log/info "Backup to" output-path "complete."))
