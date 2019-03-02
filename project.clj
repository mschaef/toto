(defproject toto "0.5.1"
  :description "Toto To-Do List Manager"
  :license { :name "Copyright East Coast Toolworks (c) 2012-2018"}

  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/tools.logging "0.4.1"]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [com.mschaef/sql-file "0.3.0"]
                 [cprop "0.1.13"]
                 [yesql "0.5.3"]
                 [org.clojure/data.json "0.2.6"]
                 [clj-http "3.9.1"]
                 [hiccup "1.0.5"]
                 [ring/ring-jetty-adapter "1.7.1"]
                 [slester/ring-browser-caching "0.1.1"]
                 [com.cemerick/friend "0.2.3"]
                 [compojure "1.6.1"]
                 [slingshot "0.12.2"]]

  :plugins [[lein-tar "3.3.0"]]
  
  :tar {:uberjar true
        :format :tar-gz
        :output-dir "."
        :leading-path "toto-install"}
  
  :main toto.main
  :aot [toto.main]
  
  :ring {:handler toto.handler/handler }

  :jvm-opts ["-Dconf=local-config.edn"]
  
  :jar-name "toto.jar"
  :uberjar-name "toto-standalone.jar"

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "--no-sign"]
                  ["uberjar"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]
                  ["tar"]])
