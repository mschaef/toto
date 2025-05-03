(defproject toto "1.4.40"
  :description "Toto To-Do List Manager"
  :license { :name "Copyright East Coast Toolworks (c) 2012-2025"}

  :dependencies [[org.clojure/clojure "1.12.0"]
                 [commons-io "2.18.0"]
                 [com.mschaef/sql-file "0.4.12"]
                 [cprop "0.1.20"]
                 [yesql "0.6.0-alpha1"]
                 [org.clojure/data.json "2.5.0"]
                 [clj-http "3.13.0"]
                 [hiccup "1.0.5"]
                 [ring/ring-jetty-adapter "1.13.0"]
                 [ring/ring-devel "1.13.0"]
                 [ring/ring-json "0.5.1"]
                 [slester/ring-browser-caching "0.1.1"]
                 [com.cemerick/friend "0.2.3"]
                 [compojure "1.7.1"]
                 [joda-time/joda-time "2.13.0"]
                 [com.draines/postal "2.0.5"]
                 [it.sauronsoftware.cron4j/cron4j "2.2.5"]
                 [com.mschaef/playbook "0.1.5"]
                 [software.amazon.awssdk/s3  "2.29.19"]]

  :plugins [[lein-tar "3.3.0"]
            [dev.weavejester/lein-cljfmt "0.13.0"]]

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

  :release-tasks [["cljfmt" "check"]
                  ["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "--no-sign"]
                  ["tar"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]])
