(ns toto.dumper
  (:use toto.util
        sql-file.sql-util)
  (:require [clojure.tools.logging :as log]
            [clojure.java.jdbc :as jdbc]
            [toto.data :as data]))

(defn query [ sql ]
  (query-all data/*db* sql))

(defn sanitize-string [ string ]
  (apply str (repeat (count string) ".")))

(defn item-events []
  (let [items (query "SELECT item_id, todo_list_id, desc, is_deleted, is_complete, created_on, updated_on from TODO_ITEM")
        deletes (filter #(or (:is_deleted %)
                             (:is_complete %))
                        items)
        remaining-ids (clojure.set/difference (set (map :item_id items))
                                              (set (map :item_id deletes)))]
    (concat
     (sort-by :event-time
              (concat (map #(merge {} {:event-time (:created_on %)
                                       :event-type :add-item
                                       :item_id (:item_id %)
                                       :list_id (:todo_list_id %)
                                       :desc (sanitize-string (:desc %))})
                           items)
                      (map #(merge {} {:event-time (:updated_on %)
                                       :event-type :delete-item
                                       :item_id (:item_id %)
                                       :list_id (:todo_list_id %)})
                           deletes)))
     (map #(merge {} {:event-type :delete-item
                      :item_id (:item_id %)
                      :list_id (:todo_list_id %)})
          (filter #(remaining-ids (:item_id %)) items)))))


(def dump-file "dump-file.edn")

(defn append-to-dump [ msg ]
  (spit dump-file msg :append true)
  (spit dump-file "\n" :append true))

(defn add-list-events []
  (map #(merge {} {:event-type :add-list
                   :list_id (:todo_list_id %)
                   :desc (sanitize-string (:desc %))}) (query "SELECT todo_list_id, desc from TODO_LIST")))

(defn delete-list-events []
    (map #(merge {} {:event-type :delete-list
                     :list_id (:todo_list_id %)
                     }) (query "SELECT todo_list_id, desc from TODO_LIST")))

(defn dump-simple-event-stream [ config ]
  (log/info "dumping to " dump-file)
  (data/with-db-connection

    (doseq [ event (concat (add-list-events)
                           (item-events)
                           (delete-list-events)) ]
      (append-to-dump event))

    (log/info "finished dumping to " dump-file)))
