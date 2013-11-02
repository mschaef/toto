(ns toto.main
  (:gen-class :main true)
  (:require [clojure.tools.logging :as log]
            [toto.handler :as handler]
            [ring.adapter.jetty :as jetty]
            [toto.schema :as schema]))

(defn -main [& args]
  (log/trace "Starting Toto")
  (schema/ensure-schema-available)
  (log/trace "Starting Toto Webserver")
  (jetty/run-jetty handler/handler { :port 8080 :join? false }))
