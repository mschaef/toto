(ns toto.main
  (:gen-class :main true)
  (:require [clojure.tools.logging :as log]
            [ring.adapter.jetty :as jetty]
            [toto.core :as core]
            [toto.schema :as schema]
            [toto.handler :as handler]))

(defn -main [& args]
  (log/info "Starting Toto")
  (let [http-port (core/config-property "http.port" 8080)]
    (schema/ensure-schema-available)
    (log/info "Starting Toto Webserver on port" http-port)
    (jetty/run-jetty handler/handler
                     { :port http-port :join? false })))