(ns toto.core
  (:require [cemerick.friend :as friend]))

(defn authenticated-username []
  (if-let [cauth (friend/current-authentication)]
    (cauth :identity)
    nil))

