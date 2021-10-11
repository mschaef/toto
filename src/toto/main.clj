(ns toto.main
  (:gen-class :main true)
  (:use toto.core.util
        compojure.core
        [ring.middleware resource])
  (:require [clojure.tools.logging :as log]
            [sql-file.core :as sql-file]
            [toto.core.config :as config]
            [toto.core.data :as data]
            [toto.dumper.dumper :as dumper]
            [toto.site :as site]))

(defn start-scheduler [ ]
  (doto (it.sauronsoftware.cron4j.Scheduler.)
    (.setDaemon true)
    (.start)))

(def date-format (java.text.SimpleDateFormat. "yyyyMMdd-hhmm"))

(defn get-backup-filename []
  (str
   "toto-backup-"
   (.format date-format (java.util.Date.))
   ".tgz"))

(defn schedule-backup [config db-conn scheduler]
  (log/info :config config)
  (let [backup-path (get-in config [:db :backup-path] false)
        backup-cron (get-in config [:db :backup-cron])]
    (if backup-path
      (do
        (log/info "Automatic backup enabled and writing to" backup-path
                  (str "(cron: " backup-cron ")"))
        (.schedule scheduler backup-cron
                   (fn []
                     (data/with-db-connection db-conn
                       (data/backup-database (str backup-path "/" (get-backup-filename)))))))
      (log/warn "NO BACKUP PATH. AUTOMATIC BACKUP DISABLED!!!"))))

(defn site-start [ config db-conn ]
  (let [ scheduler (start-scheduler)]
    (schedule-backup config db-conn scheduler)
    (site/site-start config db-conn)))

(defn app-start [ config ]
  (sql-file/with-pool [db-conn (data/db-conn-spec config)]
    (case (:mode (:app config))
      :dump-simple-event-stream (dumper/dump-simple-event-stream config db-conn)
      :site (site-start config db-conn))))

(defn -main [& args]
  (let [config (config/load-config "Toto" args)]
    (log/info "Starting App" (:app config))
    (when (:development-mode config)
      (log/warn "=== DEVELOPMENT MODE ==="))
    (app-start config)
    (log/info "end run.")))
