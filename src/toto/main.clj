(ns toto.main
  (:gen-class :main true)
  (:use toto.core.util
        compojure.core
        [ring.middleware resource])
  (:require [clojure.tools.logging :as log]
            [sql-file.core :as sql-file]
            [toto.core.config :as config]
            [toto.data.data :as data]
            [toto.dumper.dumper :as dumper]
            [toto.site :as site]))

(defn app-start [ config ]
  (sql-file/with-pool [db-conn (data/db-conn-spec config)]
    (case (:mode (:app config))
      :dump-simple-event-stream (dumper/dump-simple-event-stream config db-conn)
      :site (site/site-start config db-conn))))

(defn -main [& args]
  (let [config (config/load-config "Toto" args)]
    (log/info "Starting App" (:app config))
    (when (:development-mode config)
      (log/warn "=== DEVELOPMENT MODE ==="))
    (app-start config)
    (log/info "end run.")))
