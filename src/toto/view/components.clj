(ns toto.view.components
  (:use toto.view.query
        hiccup.def)
  (:require [clojure.data.json :as json]
            [clojure.tools.logging :as log]))

(defelem scroll-column [ title & contents ]
  [:div.scroll-column
   [:div.fixed title]
   [:div.scrollable { :data-preserve-scroll "true" }
    contents]])

(defn post-button [ attrs body ]
  (let [ target (:target attrs)]
    [:span.clickable.post-button
     (cond-> {:onclick (if-let [next-url (:next-url attrs)]
                         (str "doPost('" target "'," (json/write-str (get attrs :args {})) ", '" next-url "')")
                         (str "doPost('" target "'," (json/write-str (get attrs :args {})) ")"))}
       (:shortcut-key attrs) (merge {:data-shortcut-key (:shortcut-key attrs)
                                     :data-target target}))
     body]))

