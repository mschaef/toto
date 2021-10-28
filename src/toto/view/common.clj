(ns toto.view.common
  (:use toto.core.util
        [slingshot.slingshot :only (throw+ try+)]))

;;; Development Mode

(def ^:dynamic *config* {})
(def ^:dynamic *dev-mode* false)

(defn wrap-config [ app config ]
  (fn [ req ]
    (binding [*config* config
              *dev-mode* (:development-mode config)]
      (app req))))

;;; Validation

(defmacro catch-validation-errors [ & body ]
  `(try+
    ~@body
    (catch [ :type :form-error ] details#
      ( :markup details#))))

(defn fail-validation [ markup ]
  (throw+ { :type :form-error :markup markup }))

;;; HTML Utilities

(defn class-set [ classes ]
  (clojure.string/join " " (map str (filter #(classes %)
                                            (keys classes)))))



