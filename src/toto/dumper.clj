(ns toto.dumper
  (:use toto.util
        sql-file.sql-util)
  (:require [clojure.tools.logging :as log]
            [clojure.java.jdbc :as jdbc]
            [toto.data :as data]))

(defn query [ sql ]
  (query-all data/*db* sql))

(defn item-events []
  (let [items (query "SELECT item_id, todo_list_id, desc, is_deleted, is_complete, created_on, updated_on from TODO_ITEM")
        deletes (filter #(or (:is_deleted %)
                             (:is_complete %))
                        items)
        remaining-ids (clojure.set/difference (set (map :item_id items))
                                              (set (map :item_id deletes)))]
    (concat
     (sort-by :event-time
              (concat (map #(merge % {:event-time (:created_on %)
                                      :event-type :add-item}) items)
                      (map #(merge {} {:item_id (:item_id %)
                                       :event-time (:updated_on %)
                                       :event-type :delete-item}) deletes)))
     (map #(merge {} {:item_id %
                      :event-type :delete-item})
          remaining-ids))))


(def dump-file "dump-file.edn")

(defn append-to-dump [ msg ]
  (spit dump-file msg :append true)
  (spit dump-file "\n" :append true))

(defn dump-simple-event-stream [ config ]
  (log/info "dumping to " dump-file)
  (data/with-db-connection

    (doseq [ list-info (query "SELECT todo_list_id, desc from TODO_LIST")]
      (append-to-dump (assoc list-info :event-type :add-list)))

    (doseq [ item-info (item-events)]
      (append-to-dump item-info))

    (doseq [ list-info (query "SELECT todo_list_id, desc from TODO_LIST")]
      (append-to-dump (assoc list-info :event-type :delete-list)))
    (log/info "finished dumping to " dump-file)))
