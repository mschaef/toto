(ns toto.core
  (:require [cemerick.friend :as friend]
            [clojure.string :as string]))

(defn authenticated-username []
  (if-let [cauth (friend/current-authentication)]
    (cauth :identity)
    nil))

(defn report-unauthorized []
  (friend/throw-unauthorized (friend/current-authentication) {}))

(defn query-param [ req param-name ]
  (let [params (:params req)]
    (if (nil? params)
      nil
      (params param-name))))

