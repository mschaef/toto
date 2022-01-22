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


(ns toto.core.web
  (:gen-class :main true)
  (:use toto.core.util
        compojure.core
        sql-file.middleware
        [ring.middleware resource
         not-modified
         content-type
         browser-caching])
  (:require [clojure.tools.logging :as log]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.reload :as ring-reload]
            [ring.middleware.file-info :as ring-file-info]
            [ring.middleware.resource :as ring-resource]
            [ring.util.response :as ring-responsed]
            [compojure.handler :as handler]
            [toto.core.session :as store]
            [toto.view.common :as view-common]
            [toto.view.query :as view-query]
            [toto.site.user :as user]))

(defn- wrap-request-logging [ app development-mode? ]
  (fn [req]
    (if development-mode?
      (log/debug 'REQUEST (:request-method req) (:uri req) (:params req) (:headers req))
      (log/debug 'REQUEST (:request-method req) (:uri req)))

    (let [resp (app req)]
      (if development-mode?
        (log/trace 'RESPONSE (dissoc resp :body))
        (log/trace 'RESPONSE (:status resp)))
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

(defn- wrap-dev-support [ handler dev-mode ]
  (cond-> (wrap-request-logging handler dev-mode)
    dev-mode (ring-reload/wrap-reload)))

(defn handler [ config routes ]
  (-> routes
      (wrap-content-type)
      (wrap-browser-caching {"text/javascript" 360000
                             "text/css" 360000})
      (user/wrap-authenticate)
      (extend-session-duration 30)
      (include-requesting-ip)
      (view-query/wrap-remember-query)
      (wrap-dev-support (:development-mode config))
      (handler/site {:session {:store (store/session-store (:db-conn-pool config))}})
      (wrap-db-connection (:db-conn-pool config))
      (view-common/wrap-config config)))

(defn start-site [ config routes ]
  (let [ { http-port :http-port } config ]
    (log/info "Starting Webserver on port" http-port)
    (let [server (jetty/run-jetty (handler config routes ) { :port http-port :join? false })]
      (add-shutdown-hook
       (fn []
         (log/info "Shutting down webserver")
         (.stop server)))
      (.join server))))

