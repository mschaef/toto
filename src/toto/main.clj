(ns toto.main
  (:gen-class :main true)
  (:use toto.core.util
        compojure.core
        [ring.middleware resource])
  (:require [clojure.tools.logging :as log]
            [sql-file.core :as sql-file]
            [toto.core.config :as config]
            [toto.core.data :as data]
            [toto.site.main :as site]
            [toto.core.scheduler :as scheduler]
            [toto.todo.todo :as todo]))

(defn db-conn-spec [ config ]
  (let [ spec {:name (or (config-property "db.subname")
                         (get-in config [:db :subname] "toto"))
               :schema-path [ "sql/" ]
               :schemas [[ "toto" 9 ]]}]
    (log/info "DB Conn Spec: " spec)
    spec))

(defn schedule-backup [ config ]
  (let [backup-cron (get-in config [:db :backup-cron])]
    (if-let [backup-path (get-in config [:db :backup-path] false)]
      (scheduler/schedule-job config (str "Automatic backup to " backup-path) backup-cron
                              #(data/backup-database backup-path))
      (log/warn "NO BACKUP PATH. AUTOMATIC BACKUP DISABLED!!!")))
  config)

(defn app-start [ config app-routes ]
  (sql-file/with-pool [db-conn (db-conn-spec config)]
    (-> config
        (assoc :db-conn-pool db-conn)
        (scheduler/start)
        (schedule-backup)
        (site/site-start db-conn app-routes))))

(defn -main [& args]
  (let [config (config/load-config)]
    (log/info "Starting App" (:app config))
    (when (:development-mode config)
      (log/warn "=== DEVELOPMENT MODE ==="))
    (app-start config (todo/all-routes config))
    (log/info "end run.")))
