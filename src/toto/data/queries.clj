;; Copyright (c) 2015-2025 Michael Schaeffer (dba East Coast Toolworks)
;;
;; Licensed as below.
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;       http://www.apache.org/licenses/LICENSE-2.0
;;
;; The license is also includes at the root of the project in the file
;; LICENSE.
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.
;;
;; You must not remove this notice, or any other, from this software.

(ns toto.data.queries
  (:use sql-file.middleware)
  (:require [taoensso.timbre :as log]
            [yesql.core :refer [defqueries]]
            [yesql.middleware :as middleware]))

(defn log-query-middleware [query-fn]
  (fn [args call-options]
    (let [begin-t (System/currentTimeMillis)
          query-name (get-in call-options [:query :name])]
      (log/debug [:begin query-name args])
      (let [result (query-fn args call-options)]
        (log/debug [:end query-name (- (System/currentTimeMillis) begin-t)])
        result))))

(def query-middleware (comp (middleware/set-connection current-db-connection)
                            log-query-middleware))

(defqueries "toto/data/queries.sql"
  {:middleware query-middleware})
