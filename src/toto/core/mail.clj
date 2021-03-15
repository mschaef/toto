(ns toto.core.mail
  (:use hiccup.core)
  (:require [clojure.tools.logging :as log]
            [postal.core :as postal]))

(defn send-email [config {to :to
                          subject :subject
                          hiccup-content :content}]
  (log/info "Seing mail to " to " with subject: " subject)
  (let [smtp (:smtp config)]
    (if (:enabled smtp)
      (postal/send-message {:host (:host smtp)
                            :user (:user smtp)
                            :pass (:password smtp)
                            :ssl true}
                           {:from (:from smtp)
                            :to to
                            :subject subject
                            :body [{:type "text/html"
                                    :content (html [:html hiccup-content])}]})
      (log/warn "E-mail disabled. Message not sent."))))
