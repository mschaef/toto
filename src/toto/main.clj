(ns toto.main
  (:gen-class :main true)
  (:require [clojure.tools.logging :as log]
            [toto.handler :as handler]
            [ring.adapter.jetty :as jetty]))

(defn -main [& args]
  (log/trace "Starting Toto")
  (jetty/run-jetty handler/handler {:port 8080}))
