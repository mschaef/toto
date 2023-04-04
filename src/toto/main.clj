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

(ns toto.main
  (:gen-class :main true)
  (:use toto.core.util)
  (:require [clojure.tools.logging :as log]
            [sql-file.core :as sql-file]
            [toto.core.config :as config]
            [toto.core.backup :as backup]
            [toto.site.main :as site]
            [toto.core.scheduler :as scheduler]
            [toto.todo.todo :as todo]))

(defn db-conn-spec [ config ]
  {:name (or (config-property "db.subname")
             (get-in config [:db :subname] "toto"))
   :schema-path [ "sql/" ]
   :schemas [[ "toto" 11 ]]})

(defn app-start [ config app-routes ]
  (sql-file/with-pool [db-conn (db-conn-spec config)]
    (-> config
        (assoc :db-conn-pool db-conn)
        (scheduler/start)
        (backup/schedule-backup)
        (site/site-start db-conn app-routes))))

(defn -main [& args]
  (let [config (config/load-config)]
    (log/info "Starting App" (:app config))
    (when (:development-mode config)
      (log/warn "=== DEVELOPMENT MODE ==="))
    (app-start config (todo/all-routes config))
    (log/info "end run.")))
