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

(ns base.scheduler
  (:use playbook.core
        sql-file.middleware)
  (:require [taoensso.timbre :as log]
            [playbook.config :as config]))

(defn start [ ]
  (doto (it.sauronsoftware.cron4j.Scheduler.)
    (.setDaemon true)
    (.start)))

(defn schedule-job [ scheduler job-name job-fn ]
  (if-let [ cron (config/cval :job-schedule job-name)]
    (let [job-lock (java.util.concurrent.locks.ReentrantLock.)]
      (log/info "Background job scheduled (cron:" cron  "):" job-name )
      (.schedule scheduler cron
                 #(if (.tryLock job-lock)
                    (try
                      (with-exception-barrier (str "scheduled job:" job-name)
                        (log/info "Scheduled job:" job-name)
                        (job-fn))
                      (finally
                        (.unlock job-lock)))
                    (log/info "Cannot run scheduled job reentrantly:" job-name))))
    (log/warn "Background job not scheduled, no job-schedule entry:" job-name))
  scheduler)
