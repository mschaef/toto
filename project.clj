(defproject toto "0.4.3"
  :description "Toto To-Do List Manager"
  :license { :name "Copyright East Coast Toolworks (c) 2012-2018"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.logging "0.4.0"]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [sql-file "0.2.0-pre1"]
                 [yesql "0.5.3"]
                 [org.clojure/data.json "0.2.6"]
                 [clj-http "3.7.0"]
                 [hiccup "1.0.5"]
                 [ring/ring-jetty-adapter "1.6.2"]
                 [slester/ring-browser-caching "0.1.1"]
                 [com.cemerick/friend "0.2.3"]
                 [compojure "1.6.0"]
                 [slingshot "0.12.2"]]

  :main toto.main
  :aot [toto.main]
  
  :ring {:handler toto.handler/handler }

  :jar-name "toto.jar"
  :uberjar-name "toto-standalone.jar"


  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "--no-sign"]
                  ["uberjar"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]])
