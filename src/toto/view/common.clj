;; Copyright (c) 2015-2022 Michael Schaeffer (dba East Coast Toolworks)
;;
;; Licensed as below.
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;       http://www.apache.org/licenses/LICENSE-2.0
;;
;; The license is also includes at the root of the project in the file
;; LICENSE.
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.
;;
;; You must not remove this notice, or any other, from this software.

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



