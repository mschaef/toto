(defproject toto "0.3.1-SNAPSHOT"
  :description "Toto To-Do List Manager"
  :license { :name "Copyright East Coast Toolworks (c) 2012-2016"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [ch.qos.logback/logback-classic "1.1.7"]
                 [sql-file "0.2.0-pre0"]
                 [yesql "0.5.3"]
                 [org.clojure/data.json "0.2.6"]
                 [clj-http "3.0.1"]
                 [hiccup "1.0.5"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [slester/ring-browser-caching "0.1.1"]
                 [com.cemerick/friend "0.2.1"]
                 [compojure "1.5.0"]
                 [slingshot "0.12.2"]]

  :main toto.main
  :aot [toto.main]
  
  :ring {:handler toto.handler/handler }

  :jar-name "toto.jar"
  :uberjar-name "toto-standalone.jar")
