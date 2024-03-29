;; Copyright (c) 2015-2023 Michael Schaeffer (dba East Coast Toolworks)
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


(ns toto.view.request-date
  (:use toto.core.util)
  (:require [clojure.tools.logging :as log]))

 ;;; Persistent Query

(def ^:dynamic *request-date* nil)

(defn wrap-remember-request-date [ app ]
  (fn [ req ]
    (binding [ *request-date* (current-time) ]
      (app req))))

(defn valentines-day? []
  (and *request-date*
       (= 1 (.getMonth *request-date*))
       (let [day (.getDate *request-date*)]
         (or (= day 14) (= day 15)))))
