(ns plugh.http.server
  (:gen-class)
  (:use aleph.http lamina.core plugh.util.misc)
  (:require [plugh.util.misc :as pmisc])
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

(defn map-merge [the-map [k v]]
  (conj the-map [k (conj (get the-map k []) v)]))

(defn query-split [query]
  "Take a query and turn it into a map of arrays of query values"
  (if query
    (reduce
      map-merge
      {}      
      (map 
        (fn [s] 
          (map
            url-decode
            (. s split "=")))
        (. query split "&")))
    {}))
  

;; FIXME parse the path into a Vector and extract the params
(defn- fix_req [r]
  "Parse the path into a Vector"
  (conj 
    r 
    [:path (path-split (:uri r))]
    [:query (query-split (:query-string r))]
    ))

(def stuff1 
  (match-pfunc
    [{:path [(:or "cat" "sloth") & rest]
      :query q}]
    {:status 200
     :headers {"content-type" "text/html"}
     :body (str "cat: " rest " query " q)}))

(def stuff2
  (match-pfunc
    [{:path [(:or "moose" "sloth") & rest]
      :query q}]
    {:status 200
     :headers {"content-type" "text/html"}
     :body (str "moose: " rest " query " q)}))

(def stuff (or-else stuff1 stuff2))

(defn hello-world [channel _request]
  (let [request (fix_req _request)]
    (enqueue channel
             (if (stuff :defined? request)
               (stuff request)           
               {:status 200
                :headers {"content-type" "text/html"}
                :body (str request)}))))


(def server-stopper (atom nil))

(defn run-server [port]
  (println "Running server on " port)
  (reset! server-stopper (start-http-server hello-world {:port port}))
  (println "Yep"))

(defn stop-server []
  (let [fn @server-stopper]
    (if 
      fn 
      (do
         (fn)
          (reset! server-stopper nil)))))

(comment
  "
  Okay... so how does this work in light of core.async?
  
  * Are sessions simply channels that lead to something that has state?
  * Comet is a channel that pushes to the client... maybe... or just a queue?
  * return types to deal with: something with 
      * status (response)
      * something that looks like HTML
      * something that looks like JSON
      * an IDeref thingy that turns into one of the above
  * Need to check if it's a static asset
  * Yield generated JavaScript (via ClojureScript)
  "
  )
