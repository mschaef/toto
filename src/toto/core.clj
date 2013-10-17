(ns toto.core
  (:require [cemerick.friend :as friend]
            [clojure.string :as string]))

(defn authenticated-username []
  (if-let [cauth (friend/current-authentication)]
    (cauth :identity)
    nil))

(def ^:dynamic *in-mobile-request* false)

(defn mobile-user-agent? [ user-agent ]
  (and false ; disabled for now
       (string? user-agent)
       (.contains (string/lower-case user-agent) "mobile")))

(defn query-param [ req param-name ]
  (let [params (:params req)]
    (if (nil? params)
      nil
      (params param-name))))

(defn is-mobile-request? []
  *in-mobile-request*)

(defn wrap-mobile-detect [ app ]
  (fn [ req ]
    (let [ forced-mobile? (= (query-param req :mobile) "yes")]
      (binding [*in-mobile-request* (or (mobile-user-agent? ((:headers req) "user-agent"))
                                        forced-mobile?)]
        (app req)))))
