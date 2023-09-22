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

(ns toto.site.main
  (:gen-class :main true)
  (:use playbook.core
        compojure.core)
  (:require [taoensso.timbre :as log]
            [sql-file.core :as sql-file]
            [toto.core.scheduler :as scheduler]
            [toto.core.backup :as backup]
            [toto.core.web :as web]
            [toto.data.data :as data]
            [toto.todo.sunset :as sunset]
            [toto.site.routes :as routes]
            [toto.todo.todo :as todo]))

(defn- schedule-web-session-cull [ config ]
  (scheduler/schedule-job config :web-session-cull "19 */1 * * *"
                          #(data/delete-old-web-sessions)))

(defn- schedule-item-sunset-job [ config ]
  (scheduler/schedule-job config :item-sunset "13 */1 * * *"
                          #(sunset/item-sunset-job)))

(defn- schedule-verification-link-cull [ config ]
  (scheduler/schedule-job config :verification-link-cull "*/15 * * * *"
                          #(data/delete-old-verification-links)))

(defn- start-scheduled-jobs [ config ]
  (-> config
      (scheduler/start)
      (backup/schedule-backup)
      (schedule-verification-link-cull)
      (schedule-web-session-cull)
      (schedule-item-sunset-job)))

(defn- db-conn-spec [ config ]
  {:name (or (config-property "db.subname")
             (get-in config [:db :subname] "toto"))
   :schema-path [ "sql/" ]
   :schemas [[ "toto" 11 ]]})

(defn app-start [ config ]
  (sql-file/with-pool [db-conn (db-conn-spec config)]
    (-> config
        (assoc :db-conn-pool db-conn)
        (start-scheduled-jobs)
        (web/start-site (routes/all-routes config (todo/all-routes config))))))
