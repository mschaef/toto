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


(ns toto.core.web
  (:gen-class :main true)
  (:use playbook.core
        compojure.core
        sql-file.middleware
        [ring.middleware resource
         not-modified
         content-type
         browser-caching])
  (:require [taoensso.timbre :as log]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.reload :as ring-reload]
            [ring.middleware.file-info :as ring-file-info]
            [ring.middleware.resource :as ring-resource]
            [ring.util.response :as ring-response]
            [compojure.handler :as handler]
            [playbook.config :as config]
            [toto.core.session :as session]
            [toto.core.gdpr :as gdpr]
            [toto.view.common :as view-common]
            [toto.view.query :as view-query]
            [toto.core.site.user :as user]))

(defn- wrap-request-logging [ app development-mode? ]
  (fn [req]
    (if development-mode?
      (log/debug 'REQUEST (dissoc req :body))
      (log/debug 'REQUEST (:request-method req) (:uri req)))

    (let [resp (app req)]
      (if development-mode?
        (log/debug 'RESPONSE (dissoc resp :body))
        (log/debug 'RESPONSE (:status resp)))
      resp)))

(defn get-client-ip [req]
  (if-let [ips (get-in req [:headers "x-forwarded-for"])]
    (-> ips (clojure.string/split #",") first)
    (:remote-addr req)))

(defn- include-requesting-ip [ app ]
  (fn [req]
    (app (assoc req :request-ip (get-client-ip req)))))

(defn- extend-session-duration [ app duration-in-days ]
  (fn [req]
    (assoc (app req) :session-cookie-attrs
           {:max-age (* duration-in-days 24 3600)})))

(defn- wrap-dev-support [ app dev-mode ]
  (cond-> (wrap-request-logging app dev-mode)
    dev-mode (ring-reload/wrap-reload)))

(defn wrap-request-thread-naming [ app ]
  (fn [ req ]
    (call-with-thread-name #(app req) (str "http: " (:request-method req) " " (:uri req)))))

(defn wrap-exception-handling [ app ]
  (fn [ req ]
    (try
      (app req)
      (catch Exception ex
        (let [ ex-uuid (.toString (java.util.UUID/randomUUID)) ]
          (log/error ex (str "Unhandled exception while processing " (:request-method req)
                             " request to: " (:uri req) " (uuid: " ex-uuid ")"))
          (if (= (:uri req) "/error")
            (throw (Exception. "Double fault while processing uncaught exception." ex))
            (ring-response/redirect (str "/error?uuid=" ex-uuid))))))))

(defn handler [ db-conn-pool session-store routes ]
  (-> routes
      (wrap-content-type)
      (wrap-browser-caching {"text/javascript" 360000
                             "text/css" 360000})
      (user/wrap-authenticate)
      (extend-session-duration 30)
      (include-requesting-ip)
      (view-query/wrap-remember-query)
      (gdpr/wrap-gdpr-filter)
      (handler/site {:session {:store session-store}})
      (wrap-db-connection db-conn-pool)
      (wrap-request-thread-naming)
      (config/wrap-config)
      (wrap-dev-support (config/cval :development-mode))
      (wrap-exception-handling)))

(defn start-site [ db-conn-pool session-store routes ]
  (let [ http-port (config/cval :http-port) ]
    (log/info "Starting Webserver on port" http-port)
    (let [server (jetty/run-jetty (handler db-conn-pool session-store routes)
                                  { :port http-port :join? false })]
      (add-shutdown-hook
       (fn []
         (log/info "Shutting down webserver")
         (.stop server)))
      (.join server))))

