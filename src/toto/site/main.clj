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
        compojure.core
        sql-file.middleware)
  (:require [taoensso.timbre :as log]
            [sql-file.core :as sql-file]
            [playbook.config :as config]
            [toto.core.scheduler :as scheduler]
            [toto.core.backup :as backup]
            [toto.core.session :as session]
            [toto.core.web :as web]
            [toto.data.base :as base]
            [toto.data.data :as data]
            [toto.todo.sunset :as sunset]
            [toto.site.routes :as routes]
            [toto.todo.todo :as todo]))

(defn- start-scheduled-jobs [ db-conn-pool session-store ]
  (-> (scheduler/start)
      (backup/schedule-backup db-conn-pool)
      (scheduler/schedule-job :web-session-cull
                              #(with-db-connection db-conn-pool
                                 (session/delete-old-web-sessions session-store)))
      (scheduler/schedule-job :item-sunset
                              #(with-db-connection db-conn-pool
                                 (sunset/item-sunset-job)))
      (scheduler/schedule-job :verification-link-cull
                              #(with-db-connection db-conn-pool
                                 (base/delete-old-verification-links)))))

(defn- db-conn-spec [ config ]
  {:name (or (config/property "db.subname")
             (get-in config [:db :subname] "toto"))
   :schema-path [ "sql/" ]
   :schemas [[ "toto" 11 ]]})

(defn app-start [ config ]
  (sql-file/with-pool [db-conn (db-conn-spec config)]
    (let [ session-store (session/session-store db-conn)]
      (start-scheduled-jobs db-conn session-store)
      (web/start-site db-conn session-store (routes/all-routes (todo/all-routes))))))
