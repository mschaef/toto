(ns toto.util)

(defmacro unless [ condition & body ]
  `(when (not ~condition)
     ~@body))

(defn string-empty? [ str ]
  (or (nil? str)
      (= 0 (count (.trim str)))))

(defn in? 
  "true if seq contains elm"
  [seq elm]  
  (some #(= elm %) seq))

(defn assoc-if [ map assoc? k v ]
  (if assoc?
    (assoc map k v)
    map))

(defn string-leftmost
  ( [ string count ellipsis ]
      (let [length (.length string)
            leftmost (min count length)]
        (if (< leftmost length)
          (str (.substring string 0 leftmost) ellipsis)
          string)))

  ( [ string count ]
      (string-leftmost string count "")))

(defn shorten-url-text [ url-text target-length ]
  (let [url (java.net.URL. url-text)
        base (str (.getProtocol url)
                  ":"
                  (if-let [authority (.getAuthority url)]
                    (str "//" authority)))]
    (str base
         (string-leftmost (.getPath url)
                          (max 0 (- (- target-length 3) (.length base)))
                          "..."))))

(defn parsable-integer? [ str ]
  (try
   (Integer/parseInt str)
   (catch Exception ex
     false)))

(defn config-property 
  ( [ name ] (config-property name nil))
  ( [ name default ]
      (let [prop-binding (System/getProperty name)]
        (if (nil? prop-binding)
          default
          (if-let [ int (parsable-integer? prop-binding) ]
            int
            prop-binding)))))

(defn add-shutdown-hook [ shutdown-fn ]
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. (fn []
                               (shutdown-fn)))))