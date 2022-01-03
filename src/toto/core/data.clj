(ns toto.core.data
  (:use toto.core.util
        sql-file.middleware)
  (:require [clojure.tools.logging :as log]
            [sql-file.core :as sql-file]))

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
