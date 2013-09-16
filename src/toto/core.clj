(ns toto.core
  (:require [cemerick.friend :as friend]))

(def ^:dynamic *username* nil)

(defn wrap-username [app]
  (fn [req]
    (binding [*username*
              (if-let [cauth (friend/current-authentication req)]
                (cauth :identity)
                nil)]
      (app req))))

(defn authenticated-username []
  (and (not (nil? *username*))
       *username*))

