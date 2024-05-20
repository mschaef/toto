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

(ns toto.todo.landing-page
  (:use playbook.core
        base.view.common
        base.view.icons
        base.view.components
        base.view.query
        base.view.page)
  (:require [taoensso.timbre :as log]
            [hiccup.form :as form]))

(defn render-landing-page [ params ]
  (render-page {:page-title "The Todo List Manager"}
               [:div.page-message
                [:h1 "Toto"]
                [:p
                 "Welcome to Toto, the family to-do list manager. This is "
                 "the tool that helps you remember what you need to do, coordinate "
                 "with your family and friends, and get on with your life. It's "
                 "easy to use, does what it's supposed to do, and gets out of your "
                 "way, so you can focus on what's important to you."]
                [:ul
                 [:li "Make a separate lists to categorize tasks for for work, home, or fun."]
                 [:li "Share individual lists with your friends and family."]
                 [:li "Reorder list items to match your priorities."]
                 [:li "Star items that need special focus."]
                 [:li "Snooze to hide items that can wait a bit."]]
                (session-controls)]))
