(ns toto.core.config
  (:use toto.core.util)
  (:require [clojure.tools.logging :as log]
            [cprop.core :as cprop]
            [cprop.source :as cprop-source]))


(defn- maybe-config-file [ prop-name ]
  (if-let [prop (System/getProperty prop-name)]
    (if (.exists (java.io.File. prop))
      (do
        (log/info (str "Config file found: " prop " (specified by property: " prop-name ")"))
        (cprop-source/from-file prop))
      (do
        (log/warn (str "CONFIG FILE NOT FOUND: " prop " (specified by property: " prop-name ")"))
        {}))
    {}))

(defn load-config [ ]
  (cprop/load-config :merge [(cprop-source/from-resource "config.edn")
                             (maybe-config-file "conf")
                             (maybe-config-file "creds")]))

(def ^:dynamic *config* nil)

(defn call-with-config [ fn config ]
  (binding [ *config* config ]
    (fn)))

(defmacro with-config [ config & body ]
  `(call-with-config (fn [] ~@body) ~config))

(defn config [ & path-elem ]
  (get-in *config* path-elem))
