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

(ns toto.main
  (:gen-class :main true)
  (:use playbook.core)
  (:require [playbook.logging :as logging]
            [playbook.config :as config]
            [taoensso.timbre :as log]
            [toto.site.main :as main]))

(defn -main [& args]
  (let [config (-> (config/load-config)
                   (assoc :log-levels [[#{"hsqldb.*" "com.zaxxer.hikari.*"} :warn]]))]
    (logging/setup-logging config)
    (log/info "Starting App" (:app config))
    (when (:development-mode config)
      (log/warn "=== DEVELOPMENT MODE ==="))
    (main/app-start config)
    (log/info "end run.")))
