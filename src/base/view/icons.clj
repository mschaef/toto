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

(ns base.view.icons
  (:use base.view.common))

(def img-drag-handle [:img {:class "icon-drag-handle"
                            :src (resource "drag-vertical.svg")}])

(def img-back-arrow [:i {:class "fa fa-arrow-left icon-back-arrow"}])
(def img-arrow-blue [:i {:class "fa fa-lg fa-arrow-down icon-blue"}])
(def img-arrow-gray [:i {:class "fa fa-lg fa-arrow-down icon-gray"}])
(def img-bars [:i {:class "fa fa-bars icon-gray"}])
(def img-check [:i {:class "fa fa-check icon-black"}])
(def img-close-list [:i {:class "fa fa-times-circle fa-lg"}])
(def img-edit-list [:i {:class "fa fa-pencil icon-edit"}])
(def img-folders [:i {:class "fa fa-briefcase fa-lg icon-gray"}])
(def img-globe [:i {:class "fa fa-globe fa-lg icon-gray"}])
(def img-group [:i {:class "fa fa-group icon-gray"}])
(def img-restore [:i {:class "fa fa-repeat icon-black"}])
(def img-show-list [:i {:class "fa fa-bars fa-lg icon-bars"}])

(def img-star-gray [:i {:class "fa fa-lg fa-star-o icon-gray"}])
(def img-star-yellow [:i {:class "fa fa-lg fa-star icon-yellow"}])

(def img-heart-pink [:i {:class "fa fa-lg fa-heart icon-pink"}])
(def img-heart-red [:i {:class "fa fa-lg fa-heart icon-red"}])

(def img-trash [:i {:class "fa fa-trash-o icon-black"}])
(def img-window-close [:i {:class "fa fa-window-close icon-gray"}])

(def img-sunset [:img {:src (resource "icon-sunset.svg")
                       :width 24}])

