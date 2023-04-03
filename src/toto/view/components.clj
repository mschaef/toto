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

(ns toto.view.components
  (:use toto.view.query
        hiccup.def
        toto.core.util)
  (:require [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [hiccup.form :as form]
            [toto.view.auth :as auth]))

(defelem scroll-column [ id title & contents ]
  [:div.scroll-column
   [:div.fixed title]
   [:div.scrollable { :id id :data-preserve-scroll "true" }
    contents]])

(defn post-button [ attrs body ]
  (let [ target (:target attrs)]
    [:span.clickable.post-button
     (cond-> {:onclick (if-let [next-url (:next-url attrs)]
                         (str "window._toto.doPost('" target "'," (json/write-str (get attrs :args {})) ", '" next-url "')")
                         (str "window._toto.doPost('" target "'," (json/write-str (get attrs :args {})) ")"))}
       (:shortcut-key attrs) (merge {:data-shortcut-key (:shortcut-key attrs)
                                     :data-target target}))
     body]))

(defn render-verify-question []
  (when (not (auth/current-identity))
    (let [verify-n-1 (rand-int 100)
          verify-n-2 (rand-int 100)]
      [:div.config-panel
       [:h1 "Math Problem"]
       "Please solve this math problem to help confirm you are not a robot."
       [:div.verify-problem
        (form/hidden-field {} "verify-n-1" verify-n-1)
        (form/hidden-field {} "verify-n-2" verify-n-2)
        verify-n-1 " + " verify-n-2 " = "
        (form/text-field {:class "verify-response"} "verify-response")]])))

(defn verify-response-correct [ params ]
  (or (auth/current-identity)
      (let [{:keys [:verify-n-1 :verify-n-2 :verify-response]} params]
        (= (or (parsable-integer? verify-response) -1)
           (+ (or (parsable-integer? verify-n-1) -1)
              (or (parsable-integer? verify-n-2) -1))))))
