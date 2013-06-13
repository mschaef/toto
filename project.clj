(defproject toto "0.1.0-SNAPSHOT"
  :description "Toto To-Do List Manager"
  :license { :name "Copyright East Coast Toolworks (c) 2012-2013"}

  :plugins [[lein-ring "0.8.5"]]

  :dependencies [[org.clojure/clojure "1.4.0"]
                 [clj-http "0.6.4"]
                 [hiccup "1.0.1"]
                 [ring/ring-servlet "1.2.0-beta2"]
                 [com.cemerick/friend "0.1.5"]
                 [org.clojure/java.jdbc "0.2.3"]
                 [org.hsqldb/hsqldb "2.2.8"]
                 [compojure "1.1.5"]]

  :ring {
         :init toto.schema/ensure-schema-available
         :handler toto.handler/handler
         :nrepl { :start? true :port 53095 }
         })
