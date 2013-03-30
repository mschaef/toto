(ns toto.data
  (:require [clojure.java.io :as io]
            [clojure.java.jdbc :as sql]))

(def hsql-db {:subprotocol "hsqldb"
              :subname "file:~/ectworks/toto/toto.h2db"
              :user "sa"
              :password ""
              })

(defn all-table-names []
  (sql/with-connection hsql-db
    (sql/with-query-results rows
      ["select table_name from information_schema.tables order by table_name"]
      (doall (map :table_name rows)))))

