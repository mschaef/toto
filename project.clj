(defproject toto "0.1.0-SNAPSHOT"
  :description "Toto To-Do List Manager"
  :license {:name "Copyright East Coast Toolworks (c) 2012-2013"}
  :warn-on-reflection true
  :plugins [[lein-servlet "0.2.1-SNAPSHOT"]]
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [clj-http "0.6.4"]
                 [hiccup "1.0.1"]
                 [org.clojure/java.jdbc "0.2.3"]
                 [org.hsqldb/hsqldb "2.2.8"]]

  :aot [toto.servlet]
  :servlet {;; uncomment only either of the :deps entries below
            ;; :deps    [[lein-servlet/adapter-jetty7  "0.2.0"]]
            ;; :deps    [[lein-servlet/adapter-jetty8  "0.2.0"]]
            :deps    [[lein-servlet/adapter-tomcat7 "0.2.1-SNAPSHOT"]]
            :config  {:port 3000}
            :webapps {"/" {:servlets {"/*" 'toto.servlet}
                           :public "public"}}})
