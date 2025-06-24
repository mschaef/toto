;; Copyright (c) 2015-2025 Michael Schaeffer (dba East Coast Toolworks)
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

(ns toto.site.routes
  (:use playbook.core
        compojure.core)
  (:require [taoensso.timbre :as log]
            [compojure.route :as route]
            [hiccup.core :as hiccup-core]
            [playbook.config :as config]
            [toto.util :as util]
            [toto.site.user :as user]
            [toto.site.error-handling :as error-handling]))

(defn modal-routes [modal-map]
  (routes
   (context "/modal/:modal-id" [modal-id]
     (GET "/" params
       (when-let [modal-fn (get modal-map modal-id)]
         (hiccup-core/html
          [:turbo-frame#modal-container
           (modal-fn params)]))))))

(defn all-routes [app-routes app-modals]
  (let [resources-path (str "/" (util/get-version))]
    (log/info "Resources on path: " resources-path)
    (routes
     (route/resources resources-path)
     user/all-routes
     app-routes
     (modal-routes app-modals)
     error-handling/all-routes)))
