(ns toto.main
  (:gen-class :main true)
  (:require [clojure.tools.logging :as log]
            [ring.adapter.jetty :as jetty]
            [toto.core :as core]
            [toto.schema :as schema]
            [toto.handler :as handler]))

(defn run-webserver [ http-port ]
  (log/info "Starting Toto Webserver on port" http-port)
  (let [server (jetty/run-jetty handler/handler  { :port http-port :join? false })]
    (core/add-shutdown-hook
     (fn []
       (log/info "Shutting down webserver")
       (.stop server)))
    (.join server)))

(defn -main [& args]
  (log/info "Starting Toto")
  (schema/ensure-schema-available)
  (run-webserver (core/config-property "http.nport" 8080))
  (log/info "end run."))