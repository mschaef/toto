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

(ns toto.todo.ids
  (:use playbook.core)
  (:require [playbook.hashid :as hashid]
            [playbook.config :as config]))

(defn encode-item-id [ list-id ]
  (hashid/encode :ti list-id))

(defn decode-item-id [ list-id ]
  (or (try-parse-integer list-id)
      (hashid/decode :ti list-id)))

(defn encode-list-id [ list-id ]
  (hashid/encode :tl list-id))

(defn decode-list-id [ list-id ]
  (or (try-parse-integer list-id)
      (hashid/decode :tl list-id)))

(defn todo-list-link [ list-id ]
  (str (config/cval :base-url) "/list/" (encode-list-id list-id)))

