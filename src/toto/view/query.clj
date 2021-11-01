(ns toto.view.query)

 ;;; Persistent Query

(def ^:dynamic *query* nil)

(defn wrap-remember-query[ app ]
  (fn [ req ]
    (binding [ *query* (:query-params req) ]
      (app req))))

(defn call-with-modified-query [ mfn f ]
  (binding [ *query* (mfn *query*) ]
    (f)))

(defmacro with-modified-query [ mfn & body ]
  `(call-with-modified-query ~mfn (fn [] ~@body)))

(defn- normalize-param-map [ params ]
  (into {} (map (fn [[ param val]] [ (keyword param) val ])
                params)))

(defn shref* [ & args ]
  (let [url (apply str (remove map? args))
        query-params (apply merge (map normalize-param-map (filter map? args)))]
    (let [query-string (clojure.string/join "&" (map (fn [[ param val ]] (str (name param) "=" val)) query-params))]
      (if (> (.length query-string) 0)
        (str url "?" query-string)
        url))))

(defn shref [ & args ]
  (apply shref* (or *query* {}) args))
