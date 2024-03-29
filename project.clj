(defproject toto "1.4.24-SNAPSHOT"
  :description "Toto To-Do List Manager"
  :license { :name "Copyright East Coast Toolworks (c) 2012-2023"}

  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/tools.logging "1.2.4"]
                 [ch.qos.logback/logback-classic "1.4.6"]
                 [com.mschaef/sql-file "0.4.8"]
                 [cprop "0.1.19"]
                 [yesql "0.5.3"]
                 [org.clojure/data.json "2.4.0"]
                 [clj-http "3.12.3"]
                 [hiccup "1.0.5"]
                 [ring/ring-jetty-adapter "1.9.6"]
                 [ring/ring-devel "1.9.6"]
                 [slester/ring-browser-caching "0.1.1"]
                 [com.cemerick/friend "0.2.3"]
                 [compojure "1.7.0"]
                 [joda-time/joda-time "2.12.5"]
                 [com.draines/postal "2.0.5"]
                 [it.sauronsoftware.cron4j/cron4j "2.2.5"]]

  :plugins [[lein-tar "3.3.0"]]

  :tar {:uberjar true
        :format :tar-gz
        :output-dir "."
        :leading-path "toto-install"}

  :main toto.main
  :aot [toto.main]

  :ring {:handler toto.handler/handler }

  :jvm-opts ["-Dconf=local-config.edn"
             "-Dcreds=local-creds.edn"]

  :jar-name "toto.jar"
  :uberjar-name "toto-standalone.jar"

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "--no-sign"]
                  ["tar"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]])
