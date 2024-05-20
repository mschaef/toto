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

(ns base.backup
  (:use playbook.core
        sql-file.middleware)
  (:require [taoensso.timbre :as log]
            [sql-file.core :as sql-file]
            [base.scheduler :as scheduler]
            [playbook.config :as config]))

;;; backup

(def date-format (java.text.SimpleDateFormat. "yyyyMMdd-hhmm"))

(defn- get-backup-filename [ backup-path ]
  (str
   backup-path
   "/toto-backup-"
   (.format date-format (current-time))
   ".tgz"))

(defn backup-database [ backup-path ]
  (let [ output-path (get-backup-filename backup-path)]
    (log/info "Backing database up to" output-path)
    (sql-file/backup-to-file-online (current-db-connection) output-path)))

(defn schedule-backup [ scheduler db-conn-pool ]
  (if-let [backup-path (config/cval :db :backup-path)]
    (do
      (log/info "Database backup configured with path: " backup-path)
      (scheduler/schedule-job scheduler :db-backup
                              #(with-db-connection db-conn-pool
                                 (backup-database backup-path))))
    (log/warn "NO BACKUP PATH. AUTOMATIC BACKUP DISABLED!!!"))
  scheduler)
