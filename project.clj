(defproject toto "1.4.37-SNAPSHOT"
  :description "Toto To-Do List Manager"
  :license { :name "Copyright East Coast Toolworks (c) 2012-2023"}

  :dependencies [[org.clojure/clojure "1.11.1"]
                 [commons-io "2.15.0"]
                 [com.mschaef/sql-file "0.4.11"]
                 [cprop "0.1.19"]
                 [yesql "0.5.3"]
                 [org.clojure/data.json "2.5.0"]
                 [clj-http "3.12.3"]
                 [hiccup "1.0.5"]
                 [ring/ring-jetty-adapter "1.11.0"]
                 [ring/ring-devel "1.11.0"]
                 [ring/ring-json "0.5.1"]
                 [slester/ring-browser-caching "0.1.1"]
                 [com.cemerick/friend "0.2.3"]
                 [compojure "1.7.1"]
                 [joda-time/joda-time "2.12.7"]
                 [com.draines/postal "2.0.5"]
                 [it.sauronsoftware.cron4j/cron4j "2.2.5"]
                 [com.mschaef/playbook "0.1.2"]
                 [software.amazon.awssdk/s3  "2.23.0"]]

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
