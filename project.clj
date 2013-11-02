(defproject toto "0.2.2-SNAPSHOT"
  :description "Toto To-Do List Manager"
  :license { :name "Copyright East Coast Toolworks (c) 2012-2013"}

  :plugins [[lein-ring "0.8.7"]]

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/java.jdbc "0.2.3"]
                 [org.clojure/tools.logging "0.2.6"]
                 [log4j/log4j "1.2.16"]
                 [org.hsqldb/hsqldb "2.3.0"]
                 [clj-http "0.6.4"]
                 [hiccup "1.0.4"]
                 [ring/ring-jetty-adapter "1.2.1"]
                 [com.cemerick/friend "0.2.0"]
                 [compojure "1.1.6"]
                 [slingshot "0.10.3"]]

  :main toto.main

  :ring {
         :init toto.schema/ensure-schema-available
         :handler toto.handler/handler
         :nrepl { :start? true :port 53095 }
         }

  )
