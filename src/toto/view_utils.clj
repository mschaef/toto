(ns toto.view-utils)

(def img-group [:i {:class "fa fa-group icon-gray"}])

(def img-star-gray [:i {:class "fa fa-lg fa-star-o icon-gray"}])
(def img-star-yellow [:i {:class "fa fa-lg fa-star icon-yellow"}])

(def img-arrow-gray [:i {:class "fa fa-lg fa-arrow-down icon-gray"}])
(def img-arrow-blue [:i {:class "fa fa-lg fa-arrow-down icon-blue"}])

(def img-edit-list [:i {:class "fa fa-pencil icon-edit"}])

(def img-check [:i {:class "fa fa-check icon-black"}])
(def img-trash [:i {:class "fa fa-trash-o icon-black"}])
(def img-restore [:i {:class "fa fa-repeat icon-black"}])

(defmacro catch-validation-errors [ & body ]
  `(try+
    ~@body
    (catch [ :type :form-error ] details#
      ( :markup details#))))

(defn fail-validation [ markup ]
  (throw+ { :type :form-error :markup markup }))

(defn- report-unauthorized []
  (friend/throw-unauthorized (friend/current-authentication) {}))
