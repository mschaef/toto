(defproject toto "0.1.12-SNAPSHOT"
  :description "Toto To-Do List Manager"
  :license { :name "Copyright East Coast Toolworks (c) 2012-2013"}

  :plugins [[lein-ring "0.8.5"]]

  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/java.jdbc "0.2.3"]
                 [org.clojure/tools.logging "0.2.6"]
                 [log4j/log4j "1.2.16"]
                 [org.hsqldb/hsqldb "2.3.0"]
                 [clj-http "0.6.4"]
                 [hiccup "1.0.1"]
                 [ring/ring-servlet "1.2.0-beta2"]
                 [com.cemerick/friend "0.1.5"]
                 [compojure "1.1.5"]
                 [slingshot "0.10.2"]]

  :ring {
         :init toto.schema/ensure-schema-available
         :handler toto.handler/handler
         :nrepl { :start? true :port 53095 }
         })
