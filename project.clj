(defproject toto "0.2.3-SNAPSHOT"
  :description "Toto To-Do List Manager"
  :license { :name "Copyright East Coast Toolworks (c) 2012-2015"}

  :plugins [[lein-ring "0.9.0"]]

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/java.jdbc "0.3.5"]
                 [org.clojure/tools.logging "0.3.1"]
                 [ch.qos.logback/logback-classic "1.0.9"]
                 [org.hsqldb/hsqldb "2.3.2"]
                 [clj-http "1.0.1"
                  :exclusions [com.fasterxml.jackson.core/jackson-core]]
                 [hiccup "1.0.5"]
                 [ring/ring-jetty-adapter "1.3.2"]
                 [com.cemerick/friend "0.2.1"]
                 [compojure "1.3.1"]
                 [slingshot "0.12.1"]]

  :main toto.main

  :ring {
         :init toto.schema/ensure-schema-available
         :port 8080
         :handler toto.handler/handler
         :nrepl { :start? true :port 53095 }
         }

  :jar-name "toto.jar"
  :uberjar-name "toto-standalone.jar"
  )
