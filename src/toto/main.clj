(ns toto.main
  (:require [toto.handler :as handler]
            [ring.adapter.jetty :as jetty]))

(defn -main [& args]
  (jetty/run-jetty handler/handler {:port 8080}))
