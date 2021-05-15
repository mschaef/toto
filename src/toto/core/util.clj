(ns toto.core.util
  (:require [clojure.tools.logging :as log]
            [clojure.java.jdbc :as jdbc]
            [hiccup.util :as util]))

(defmacro get-version []
  ;; Capture compile-time property definition from Lein
  (System/getProperty "toto.version"))

(defmacro unless [ condition & body ]
  `(when (not ~condition)
     ~@body))

(defn string-empty? [ str ]
  (or (nil? str)
      (= 0 (count (.trim str)))))

(defn partition-string [ string n ]
  "Partition a full string into segments of length n, returning a
  sequence of strings of at most that length."
  (map (partial apply str) (partition-all n string)))

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

(defn parsable-integer? [ str ]
  (try
   (Integer/parseInt str)
   (catch Exception ex
     false)))

;;; Date utilities

(defn add-days [ date days ]
  (let [c (java.util.Calendar/getInstance)]
    (.setTime c date)
    (.add c java.util.Calendar/DATE days)
    (.getTime c)))

;;; Configuration properties

(defn config-property
  ( [ name ] (config-property name nil))
  ( [ name default ]
      (let [prop-binding (System/getProperty name)]
        (if (nil? prop-binding)
          default
          (if-let [ int (parsable-integer? prop-binding) ]
            int
            prop-binding)))))
