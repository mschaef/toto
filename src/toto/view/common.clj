(ns toto.view.common
  (:use toto.core.util
        clojure.set
        hiccup.core
        [slingshot.slingshot :only (throw+ try+)])
  (:require [clojure.tools.logging :as log]
            [clojure.data.json :as json]
            [cemerick.friend :as friend]
            [hiccup.form :as form]
            [hiccup.page :as page]
            [hiccup.util :as util]))

(def ^:dynamic *dev-mode* false)

(defn wrap-dev-mode [ app dev-mode ]
  (fn [ req ]
    (binding [ *dev-mode* dev-mode ]
      (app req))))

(def ^:dynamic *query* nil)

(defn wrap-remember-query[ app ]
  (fn [ req ]
    (binding [ *query* (:query-params req) ]
      (app req))))

(defn call-with-modified-query [ mfn f ]
  (binding [ *query* (mfn *query*) ]
    (f)))

(defmacro with-modified-query [ mfn & body ]
  `(call-with-modified-query ~mfn (fn [] ~@body)))

(defn- normalize-param-map [ params ]
  (into {} (map (fn [[ param val]] [ (keyword param) val ])
                params)))

(defn shref* [ & args ]
  (let [url (apply str (remove map? args))
        query-params (apply merge (map normalize-param-map (filter map? args)))]
    (let [query-string (clojure.string/join "&" (map (fn [[ param val ]] (str (name param) "=" val)) query-params))]
      (if (> (.length query-string) 0)
        (str url "?" query-string)
        url))))

(defn shref [ & args ]
  (apply shref* (or *query* {}) args))


(def img-window-close [:i {:class "fa fa-window-close-o icon-gray"}])

(def img-group [:i {:class "fa fa-group icon-gray"}])

(def img-globe [:i {:class "fa fa-globe fa-lg icon-gray"}])

(def img-star-gray [:i {:class "fa fa-lg fa-star-o icon-gray"}])
(def img-star-yellow [:i {:class "fa fa-lg fa-star icon-yellow"}])

(def img-arrow-gray [:i {:class "fa fa-lg fa-arrow-down icon-gray"}])
(def img-arrow-blue [:i {:class "fa fa-lg fa-arrow-down icon-blue"}])

(def img-edit-list [:i {:class "fa fa-pencil icon-edit"}])

(def img-check [:i {:class "fa fa-check icon-black"}])
(def img-trash [:i {:class "fa fa-trash-o icon-black"}])
(def img-restore [:i {:class "fa fa-repeat icon-black"}])

(def img-bars [:i {:class "fa fa-bars icon-gray"}])

(defmacro catch-validation-errors [ & body ]
  `(try+
    ~@body
    (catch [ :type :form-error ] details#
      ( :markup details#))))

(defn fail-validation [ markup ]
  (throw+ { :type :form-error :markup markup }))

(defn report-unauthorized []
  (friend/throw-unauthorized (friend/current-authentication) {}))

(defn render-scroll-column [ title & contents ]
  [:div.scroll-column
   [:div.fixed title]
   [:div#scroll-list.scrollable  { :data-preserve-scroll "true" }
    contents]])

(defn post-button [ target args desc body ]
  [:span.clickable
   {:onclick (str "doPost('" target "'," (json/write-str args) ")")}
   body])

(defn post-button* [ target args desc body next-url ]
  [:span.clickable
   {:onclick (str "doPost('" target "'," (json/write-str args) ", '" next-url "')")}
   body])

(defn item-priority-button [ item-id new-priority image-spec writable? ]
  (if writable?
    (post-button (str "/item/" item-id "/priority") {:new-priority new-priority}
                 "Set Priority" image-spec)
    image-spec))

(defn render-item-priority-control [ item-id priority writable? ]
  (if (<= priority 0)
    (item-priority-button item-id 1 img-star-gray writable?)
    (item-priority-button item-id 0 img-star-yellow writable?)))

(defn list-priority-button [ list-id new-priority image-spec ]
  (post-button (shref "/list/" list-id "/priority") {:new-priority new-priority}
               "Set Priority" image-spec))

(defn render-list-star-control [ list-id priority ]
  (if (<= priority 0)
    (list-priority-button list-id 1 img-star-gray)
    (list-priority-button list-id 0 img-star-yellow)))

(defn render-list-arrow-control [ list-id priority ]
  (if (>= priority 0)
    (list-priority-button list-id -1 img-arrow-gray)
    (list-priority-button list-id 0 img-arrow-blue)))

(defn current-identity []
  (if-let [auth (friend/current-authentication)]
    (:identity auth)))

(defn current-user-id []
  (if-let [ cauth (friend/current-authentication) ]
    (:user-id cauth)))

;;; HTML Utilities

(defn class-set [ classes ]
  (clojure.string/join " " (map str (filter #(classes %)
                                            (keys classes)))))

(def html-breakpoint "&#8203;")

(defn ensure-string-breakpoints [ s n ]
  (clojure.string/join html-breakpoint (partition-string s n)))

(defn ensure-string-breaks [ string at ]
  (clojure.string/replace string at (str at html-breakpoint)))

(defn shorten-url-text [ url-text target-length ]
  (let [url (java.net.URL. url-text)
        base (str (.getProtocol url)
                  ":"
                  (if-let [authority (.getAuthority url)]
                    (str "//" authority)))]
    (-> (util/escape-html
         (str base
              (string-leftmost (.getPath url)
                               (max 0 (- (- target-length 3) (.length base)))
                               "...")))
        (ensure-string-breaks "/")
        (ensure-string-breaks "."))))

(def app-name "Toto")

(def img-show-list
  [:i {:class "fa fa-bars fa-lg icon-bars"}])

(def img-close-list
  [:i {:class "fa fa-times-circle fa-lg"}])

(defn- logout-button []
  [:span.logout
   [:a { :href "/logout"} "[logout]"]])

(defn- login-button []
  [:span.login
   [:a { :href "/"} "[login]"]])

(defn- resource [ path ]
  (str "/" (get-version) "/" path))

(defn- render-standard-header [ page-title ]
  [:head
   [:meta {:name "viewport"
           ;; user-scalable=no fails to work on iOS n where n > 10
           :content "width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0"}]
   [:title app-name (when page-title (str " - " page-title))]
   [:link { :rel "shortcut icon" :href (resource "favicon.ico")}]
   (page/include-css (resource "toto.css")
                     (resource "font-awesome.min.css"))
   (page/include-js (resource "turbolinks.js"))
   (page/include-js (resource "toto.js"))
   (page/include-js (resource "DragDropTouch.js"))])

(defn- render-footer [ username ]
  [:div.footer
   "&#9400; 2020 East Coast Toolworks "
   (when username
     [:div.logout username " - " (logout-button)])])

(defn- render-header [ page-title username show-menu? ]
  [:div.header
   (when show-menu?
     [:span.toggle-menu img-show-list "&nbsp;"])
   [:span.app-name
    [:a { :href "/" } app-name] " - "]
   page-title
   (when *dev-mode*
     [:span.pill.dev "DEV"])
   (if username
     [:div.right
      [:span.logout
       [:a {:href "/user/info"} username]
       [:span.logout-control " - " (logout-button)]]]
     [:div.right
      (login-button)])])

(defn- render-sidebar [ username sidebar ]
  [:div.sidebar
   [:div.sidebar-control
    [:span.close-menu  img-close-list "&nbsp;"]
    [:span.logout
     [:a {:href "/user/password-change"} username]
     [:span.logout-control " - " (logout-button)]]]
   [:div#sidebar.sidebar-content { :data-preserve-scroll "true" }
    sidebar
    [:div.copyright
     "&#9400; 2015-2020 East Coast Toolworks "]]])

(defn- render-page-body [ page-data-class page-title username sidebar modal contents ]
  [:body (if page-data-class
           {:data-class page-data-class})
   (when modal
     [:div.modal modal])
   (render-header page-title username (not (nil? sidebar)))
   (if sidebar
     (render-sidebar username sidebar))
   [:div.contents {:class (class-set { "with-sidebar" sidebar })}
    contents]])

(defn render-page [{ :keys [ page-title page-data-class sidebar modal ] }  & contents]
  (let [username (current-identity)]
    (html [:html
           (render-standard-header page-title)
           (render-page-body page-data-class page-title username sidebar modal contents)])))

