(ns toto.core.backup
  (:use toto.core.util
        sql-file.middleware)
  (:require [clojure.tools.logging :as log]
            [sql-file.core :as sql-file]
            [toto.core.scheduler :as scheduler]))

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

(defn schedule-backup [ config ]
  (let [backup-cron (get-in config [:db :backup-cron])]
    (if-let [backup-path (get-in config [:db :backup-path] false)]
      (scheduler/schedule-job config (str "Automatic backup to " backup-path) backup-cron
                              #(backup-database backup-path))
      (log/warn "NO BACKUP PATH. AUTOMATIC BACKUP DISABLED!!!")))
  config)
