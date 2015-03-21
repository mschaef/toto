(defproject toto "0.2.7-SNAPSHOT"
  :description "Toto To-Do List Manager"
  :license { :name "Copyright East Coast Toolworks (c) 2012-2015"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [ch.qos.logback/logback-classic "1.1.2"]
                 [com.ksmpartners/sql-file "0.1.0"]
                 [clj-http "1.0.1"
                  :exclusions [com.fasterxml.jackson.core/jackson-core]]
                 [hiccup "1.0.5"]
                 [ring/ring-jetty-adapter "1.3.2"]
                 [com.cemerick/friend "0.2.1"]
                 [compojure "1.3.2"]
                 [slingshot "0.12.2"]]

  :main toto.main
  :aot [toto.main]
  
  :ring {:handler toto.handler/handler }

  :jar-name "toto.jar"
  :uberjar-name "toto-standalone.jar")
