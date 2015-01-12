(ns toto.schema
  (:use toto.util)
  (:require [clojure.tools.logging :as log]
            [clojure.java.jdbc :as jdbc]
            [sql-file.core :as sql-file]
            [toto.core :as core]))

(def db-connection (sql-file/open-hsqldb-file-conn "toto-db" "toto" 0))

(defn log-safe-db-connection []
  (dissoc db-connection :password))

(defmacro with-db-connection [ binding & body ]
  `(jdbc/with-db-connection [~binding db-connection ]
     ~@body))


