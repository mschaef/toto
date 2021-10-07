(ns toto.core.session
  (:require [clojure.tools.logging :as log]
            [ring.middleware.session.memory :refer [memory-store]]
            [ring.middleware.session.store :as store]))

(deftype LoggingStore [ target ]
  store/SessionStore

  (read-session [_ key]
    (log/debug :read-session key)
    (store/read-session target key))

  (write-session [_ key data]
    (log/debug :write-session key data)
    (store/write-session target key data))

  (delete-session [_ key]
    (log/debug :delete-session key)
    (store/delete-session target key)))

(defn session-store []
  (LoggingStore. (memory-store)))
