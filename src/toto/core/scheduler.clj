(ns toto.core.scheduler
  (:require [clojure.tools.logging :as log]
            [toto.core.data :as data]))

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
                  (data/with-db-connection (:db-conn-pool config)
                    (job-fn))))))
