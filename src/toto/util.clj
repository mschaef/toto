(ns toto.util
  (:require [clojure.tools.logging :as log]
            [clojure.java.jdbc :as jdbc]
            [hiccup.util :as util]))

(defn query-all [ db-connection query-spec ]
  (log/debug "query-all:" query-spec)
  (jdbc/query db-connection query-spec))

(defn query-first [ db-connection query-spec ]
  (log/debug "query-first:" query-spec)
  (first (jdbc/query db-connection query-spec)))

(defn query-scalar [ db-connection query-spec ]
  (log/debug "query-scalar:" query-spec)
  (let [first-row (first (jdbc/query db-connection query-spec))
        row-keys (keys first-row)]
    (when (> (count row-keys) 1)
      (log/warn "Queries used for query-scalar should only return one field per row:" query-spec))
    (get first-row (first row-keys))))

(defn do-statements [ conn stmts ]
  "Execute a sequence of statements against the given DB connection."
  (jdbc/with-db-connection [ cdb conn ]
    (doseq [ stmt stmts ]
      (log/debug "db-do-prepared:" stmt)
      (jdbc/db-do-prepared cdb stmt))))

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
    (clojure.string/replace
     (util/escape-html
      (str base
           (string-leftmost (.getPath url)
                            (max 0 (- (- target-length 3) (.length base)))
                            "...")))
     #"/" "/&#8203;")))

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


;;; HTML Utilities

(defn table-head [ & tds ]
    (let [ [ attrs tds ]
         (if (map? tds)
           [ (first tds) (rest tds) ]
           [ {} tds ])]
      `[:thead
        [:tr ~attrs ~@(map (fn [ td ] [:th td]) tds)]]))

(defn table-row [ & tds ]
  (let [ [ attrs tds ]
         (if (map? (first tds))
           [ (first tds) (rest tds) ]
           [ {} tds ])]
    `[:tr ~attrs ~@(map (fn [ td ] [:td td]) tds)]))

(defn class-set [ classes ]
  (clojure.string/join " " (map str (filter #(classes %)
                                            (keys classes)))))
