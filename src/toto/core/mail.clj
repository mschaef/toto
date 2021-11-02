(ns toto.core.mail
  (:use hiccup.core)
  (:require [clojure.tools.logging :as log]
            [postal.core :as postal]))

(defn send-email [config {to :to
                          subject :subject
                          hiccup-content :content}]
  (log/info "Sending mail to " to " with subject: " subject)
  (let [smtp (:smtp config)
        html-content (html [:html hiccup-content])]
    (cond
      (not (:enabled smtp))
      (do
        (log/warn "E-mail disabled. Message not sent. Message text: ")
        (log/warn html-content))


      (or (nil? to) (= (count to) 0))
      (do
        (log/warn "No destination e-mail address. Message not send. Message text: ")
        (log/warn html-content))

      :else
      (postal/send-message {:host (:host smtp)
                            :user (:user smtp)
                            :pass (:password smtp)
                            :ssl true}
                           {:from (:from smtp)
                            :to to
                            :subject subject
                            :body [{:type "text/html"
                                    :content html-content}]}))))
