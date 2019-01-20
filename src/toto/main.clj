(ns toto.main
  (:gen-class :main true)
  (:use toto.util
        compojure.core
        [ring.middleware resource
         not-modified
         content-type
         browser-caching])
  (:require [clojure.tools.logging :as log]            
            [ring.adapter.jetty :as jetty]
            [ring.middleware.file-info :as ring-file-info]
            [ring.middleware.resource :as ring-resource]
            [ring.util.response :as ring-response]
            [cemerick.friend :as friend]
            [cemerick.friend.workflows :as workflows]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [toto.core :as core]
            [toto.data :as data]
            [toto.todo :as todo]
            [toto.view :as view]            
            [toto.user :as user]))

(defn wrap-request-logging [ app ]
  (fn [req]
    (log/debug 'REQUEST (:request-method req) (:uri req))
    (let [resp (app req)]
      (log/trace 'RESPONSE (:status resp))
      resp)))

(defn wrap-show-response [ app label ]
  (fn [req]
    (let [resp (app req)]
      (log/trace label (dissoc resp :body))
      resp)))

(defn extend-session-duration [ app duration-in-hours ]
  (fn [req]
    (assoc (app req) :session-cookie-attrs {:max-age (* duration-in-hours 3600)})))

(defn wrap-db-connection [ app ]
  (fn [ req ]
    (data/with-db-connection db
      (app req))))

(defn user-unauthorized [ request ]
  (view/render-page { :page-title "Access Denied"}
                    [:h1 "Access Denied"]))

(defn user-unverified [ request ]
  (view/render-page { :page-title "E-Mail Unverified"}
                    [:h1 "E-Mail Unverified"]))

(defn missing-verification? [ request ]
  (= (clojure.set/difference (get-in request [:cemerick.friend/authorization-failure
                                              :cemerick.friend/required-roles])
                             (:roles (friend/current-authentication)))
     #{:metlog.role/verified}))

(defn unauthorized-handler [request]
  {:status 403
   :body ((if (missing-verification? request)
            user-unverified
            user-unauthorized)
          request)})

(defroutes all-routes
  user/public-routes
  (friend/wrap-authorize user/private-routes #{:toto.role/verified})  
  (route/resources  (str "/" (get-version)))
  todo/public-routes
  (friend/wrap-authorize todo/private-routes #{:toto.role/verified})
  (route/not-found "Resource Not Found"))

(def handler (-> all-routes
                 (wrap-content-type)
                 (wrap-browser-caching {"text/javascript" 360000
                                        "text/css" 360000})
                 (friend/authenticate {:credential-fn user/get-user-by-credentials
                                       :workflows [(workflows/interactive-form)]
                                       unauthorized-handler unauthorized-handler})
                 (extend-session-duration 168)
                 (wrap-db-connection)
                 (wrap-request-logging)
                 (handler/site)))


(defn start-webserver [ http-port ]
  (log/info "Starting Webserver on port" http-port)
  (let [server (jetty/run-jetty handler { :port http-port :join? false })]
    (add-shutdown-hook
     (fn []
       (log/info "Shutting down webserver")
       (.stop server)))
    (.join server)))

(defn -main [& args]
  (log/info "Starting Toto" (get-version))
  (start-webserver (config-property "http.port" 8080))
  (log/info "end run."))
