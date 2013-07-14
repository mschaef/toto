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

(defn wrap-request-logging [app]
  (fn [req]
    (println ['REQUEST (:uri req) (:cemerick.friend/auth-config req)])
    (let [resp (app req)]
      (println ['RESPONSE (:status resp)])
      resp)))

(defn authenticated-username []
  (and (not (nil? *username*))
       *username*))

