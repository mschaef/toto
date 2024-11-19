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
            [playbook.config :as config]
            [playbook.scheduler :as scheduler]))

(defn- s3-client [ archive-config ]
  (-> (software.amazon.awssdk.services.s3.S3Client/builder)
      (.credentialsProvider
       (software.amazon.awssdk.auth.credentials.StaticCredentialsProvider/create
        (software.amazon.awssdk.auth.credentials.AwsBasicCredentials/create
         (archive-config :user)
         (archive-config :password))))
      (.region software.amazon.awssdk.regions.Region/US_EAST_1)
      (.build)))

(defn- put-object [ archive-config file ]
  (let [s3 (s3-client archive-config)]
    (.putObject s3 (-> (software.amazon.awssdk.services.s3.model.PutObjectRequest/builder)
                       (.bucket (archive-config :s3-bucket))
                       (.key (.getName file))
                       (.build))
                (software.amazon.awssdk.core.sync.RequestBody/fromFile file))))

;;; backup

(def date-format (java.text.SimpleDateFormat. "yyyyMMdd-hhmm"))

(defn- get-backup-filename [ ]
  (format "%s/%s-backup-%s.tgz"
          (config/cval :db :backup-path)
          (config/cval :app :name)
          (.format date-format (current-time))))

(defn- backup-database [ backup-file-name ]
  (log/info "Backing database up to" backup-file-name)
  (sql-file/backup-to-file-online (current-db-connection) backup-file-name)
  (log/info "Database backup complete" backup-file-name))

(defn- archive-backup-file [ backup-file-name ]
  (when-let [ archive-config (config/cval :db :backup-archive )]
    (let [ file (java.io.File. backup-file-name) ]
      (log/info "Archiving backup file" backup-file-name)
      (put-object archive-config file)
      (log/info "File archive complete, deleting file" backup-file-name)
      (.delete file))))

(defn schedule-backup [ scheduler db-conn-pool ]
  (if-let [backup-path (config/cval :db :backup-path)]
    (do
      (log/info "Database backup configured with path: " backup-path)
      (scheduler/schedule-job scheduler :db-backup
                              #(let [ backup-path (get-backup-filename) ]
                                 (with-db-connection db-conn-pool
                                   (backup-database backup-path))
                                 (archive-backup-file backup-path))))
    (log/warn "NO BACKUP PATH. AUTOMATIC BACKUP DISABLED!!!"))
  scheduler)
