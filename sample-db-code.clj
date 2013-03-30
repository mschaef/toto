(ns tramway.core
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as sql]))

(def hsql-db {:subprotocol "hsqldb"
              :subname "file:~/ectworks/toto/toto.h2db"
              :user "sa"
              :password ""
              })

(defn test-query []
 (sql/with-connection hsql-db
   (sql/with-query-results rows
     ["SELECT * FROM user"]
     rows)))

(defn tq2 []
  (sql/with-connection hsql-db
    (sql/with-query-results rows
      ["select 1 as x from information_schema.tables where 1=0"]
      rows)))

(defn tq3 []
  (sql/with-connection hsql-db
    (sql/with-query-results rows
      ["select table_name from information_schema.tables order by table_name"]
      (doall rows))))

(defn tramway-db []
  (println (sql/with-connection hsql-db (sql/create-table
                                 :footfall
                                 [:id "INTEGER" "GENERATED ALWAYS AS IDENTITY(START WITH 1)"]
                                 [:sample_date "DATE"]
                                 [:exhibition "varchar(255)"]))))
