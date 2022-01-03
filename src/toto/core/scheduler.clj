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
