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

(ns toto.site.main
  (:gen-class :main true)
  (:use toto.core.util
        compojure.core)
  (:require [clojure.tools.logging :as log]
            [compojure.route :as route]
            [toto.core.scheduler :as scheduler]
            [toto.core.web :as web]
            [toto.site.user :as user]
            [toto.data.data :as data]
            [toto.todo.todo :as todo]))

(defn all-routes [ config app-routes ]
  (log/info "Resources on path: " (str "/" (get-version)))
  (routes
   (route/resources (str "/" (get-version)))
   (user/all-routes config)
   app-routes
   (route/not-found "Resource Not Found")))

(defn schedule-verification-link-cull [ config ]
  (scheduler/schedule-job config "Verification link cull" "*/15 * * * *"
                          #(data/delete-old-verification-links))
  config)

(defn site-start [ config db-conn app-routes ]
  (schedule-verification-link-cull config)
  (web/start-site config (all-routes config app-routes)))
