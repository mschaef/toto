(ns toto.util)

(defmacro get-version []
  ;; Capture compile-time property definition from Lein
  (System/getProperty "toto.version"))
