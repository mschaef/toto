(ns toto.servlet
  (:import (java.io            PrintWriter)
           (javax.servlet      ServletConfig)
           (javax.servlet.http HttpServletRequest HttpServletResponse))
  (:gen-class :extends javax.servlet.http.HttpServlet)
  (:use toto.core))


(defn -init
  ([this]
   (println "Servlet initialized with no params"))
  ([this ^ServletConfig config]
   (println "Servlet initialized with servlet config" config)))


(defn add-cache-suppression-headers [response]
  (.setHeader response "Expires" "0" )
  (.setHeader response "Cache-Control" "no-store, no-cache, must-revalidate, max-age=0, private" )
  (.setHeader response "Pragma" "no-cache" ))

(defn handle
  [^HttpServletRequest request ^HttpServletResponse response]
  (add-cache-suppression-headers response)
  (let [template (render-current-env-table request)]
    (.setContentType response "text/html")
    (doto ^PrintWriter (.getWriter response)
      (.println template))))

(defn respond-not-found [response]
  (.setStatus response HttpServletResponse/SC_NOT_FOUND))

(defn -doGet
  [this ^HttpServletRequest request ^HttpServletResponse response]
  (if (= (.getPathInfo request) "/")
    (handle request response)
    (respond-not-found response)))

(defn -doPost
  [this ^HttpServletRequest request ^HttpServletResponse response]
  (.setStatus response HttpServletResponse/SC_METHOD_NOT_ALLOWED))


(defn -destroy
  [this]
  (println "Servlet destroyed"))

