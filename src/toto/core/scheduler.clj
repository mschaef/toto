;; Copyright (c) 2015-2022 Michael Schaeffer (dba East Coast Toolworks)
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

(ns toto.core.scheduler
  (:use sql-file.middleware)
  (:require [clojure.tools.logging :as log]))

(defn start [ config ]
  (assoc config :scheduler
         (doto (it.sauronsoftware.cron4j.Scheduler.)
           (.setDaemon true)
           (.start))))

(defn schedule-job [ config desc cron job-fn ]
  (do
    (log/info "Background job scheduled (cron:" cron  "):" desc )
    (.schedule (:scheduler config) cron
               #(do
                  (log/debug "Running scheduled job: " desc)
                  (with-db-connection (:db-conn-pool config)
                    (job-fn))))))
