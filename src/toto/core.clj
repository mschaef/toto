(ns toto.core
  (:require [cemerick.friend :as friend]
            [clojure.string :as string]))

(defn authenticated-username []
  (if-let [cauth (friend/current-authentication)]
    (cauth :identity)
    nil))

(def ^:dynamic *in-mobile-request* false)

(defn mobile-user-agent? [ user-agent ]
  (and (string? user-agent)
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

(defn config-property 
  ( [ name ] (config-property name nil))
  ( [ name default ]
      (let [prop-binding (System/getProperty name)]
        (if (nil? prop-binding)
          default
          (binding [*read-eval* false]
            (read-string prop-binding))))))

(defn add-shutdown-hook [ shutdown-fn ]
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. (fn []
                               (shutdown-fn)))))