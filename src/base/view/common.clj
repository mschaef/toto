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

(ns base.view.common
  (:use playbook.core)
  (:require [toto.util :as util]))

;;; Ring Responses

(defn unprocessable-entity [ body ]
  {:status  422
   :headers {}
   :body    body})

;;; HTML Utilities

(defn class-set [ classes ]
  (clojure.string/join " " (map str (filter #(classes %)
                                            (keys classes)))))

;;; Resource Paths

(defn resource [ path ]
  (str "/" (util/get-version) "/" path))
