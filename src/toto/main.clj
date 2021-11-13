(ns toto.main
  (:gen-class :main true)
  (:use toto.core.util
        compojure.core
        [ring.middleware resource])
  (:require [clojure.tools.logging :as log]
            [sql-file.core :as sql-file]
            [toto.core.config :as config]
            [toto.core.data :as data]
            [toto.data.data :as data2]
            [toto.site :as site]))

(defn start-scheduler [ ]
  (doto (it.sauronsoftware.cron4j.Scheduler.)
    (.setDaemon true)
    (.start)))

(defn schedule-job [scheduler desc cron job-fn]
  (do
    (log/info "Background job scheduled (cron:" cron  "):" desc )
    (.schedule scheduler cron
               #(do
                  (log/debug "Running scheduled job: " desc)
                  (job-fn)))))

(defn schedule-backup [scheduler config db-conn]
  (let [backup-cron (get-in config [:db :backup-cron])]
    (if-let [backup-path (get-in config [:db :backup-path] false)]
      (schedule-job scheduler (str "Automatic backup to" backup-path) backup-cron
                    #(data/with-db-connection db-conn
                       (data/backup-database backup-path)))
      (log/warn "NO BACKUP PATH. AUTOMATIC BACKUP DISABLED!!!"))))

(defn schedule-verification-link-cull [ scheduler db-conn ]
  (schedule-job scheduler "Verification link cull" "*/15 * * * *"
                #(data/with-db-connection db-conn
                   (data2/delete-old-verification-links))))

(defn site-start [ config db-conn ]
  (let [ scheduler (start-scheduler)]
    (schedule-backup scheduler config db-conn)
    (schedule-verification-link-cull scheduler db-conn)
    (site/site-start config db-conn)))

(defn app-start [ config ]
  (sql-file/with-pool [db-conn (data/db-conn-spec config)]
    (case (:mode (:app config))
      :site (site-start config db-conn))))

(defn -main [& args]
  (let [config (config/load-config "Toto" args)]
    (log/info "Starting App" (:app config))
    (when (:development-mode config)
      (log/warn "=== DEVELOPMENT MODE ==="))
    (app-start config)
    (log/info "end run.")))
