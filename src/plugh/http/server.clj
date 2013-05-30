(ns plugh.http.server
  (:gen-class)
  (:use aleph.http lamina.core plugh.util.misc)
  )

(defn url-decode [string]
  "URL Decodes a String"
  (. java.net.URLDecoder decode string "UTF-8"))

(defn url-encode [string]
  "URL Encodes a String"
  (. java.net.URLEncoder encode string "UTF-8`"))

(defn path-split [path]
  "Take a path in the form /foo/bar/baz
  and convert it into a Vector of strings"
  (into []
    (map url-decode
    (filter
      #(< 1 (. %1 length))
      (map #(. %1 trim)
        (. path split "/") )))))

;; FIXME parse the path into a Vector and extract the params
(defn- fix_req [r]
  r)

(defn hello-world [channel _request]
  (let [request (fix_req _request)]
  (println "Got request " request)
  (enqueue channel
    {:status 200
     :headers {"content-type" "text/html"}
     :body "Hello World!"})))


(defn run-server [port]
  (println "Running server on " port)
  (start-http-server hello-world {:port port})
  (println "Yep"))


