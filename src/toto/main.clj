(ns toto.main
  (:gen-class :main true)
  (:use toto.util)
  (:require [clojure.tools.logging :as log]
            [ring.adapter.jetty :as jetty]
            [toto.core :as core]
            [toto.schema :as schema]
            [toto.handler :as handler]))

(defn start-webserver [ http-port ]
  (log/info "Starting Toto Webserver on port" http-port)
  (let [server (jetty/run-jetty handler/handler  { :port http-port :join? false })]
    (add-shutdown-hook
     (fn []
       (log/info "Shutting down webserver")
       (.stop server)))
    (.join server)))

(defn -main [& args]
  (log/info "Starting Toto")
  (start-webserver (config-property "http.port" 8080))
  (log/info "end run."))
