(ns toto.user
  (:use compojure.core)
  (:require [toto.view :as view]))


(defroutes public-routes
  (GET "/user" []
       (view/render-new-user-form))

  (POST "/user" {{email-addr :email_addr password :password password2 :password2} :params}
        (view/add-user email-addr password password2)))

(defroutes admin-routes
  (GET "/users" []
       (view/render-users))

  (GET "/user/:id" [id]
       (view/render-user id)))
