(ns plugh.http.server
  (:gen-class)
  (:use aleph.http 
        lamina.core
        plugh.util.file
        clojure.core.async)
  (:require 
    [plugh.util.misc :as pmisc]
    [clojure.java.io :as io])
  
  )



(def server-stopper 
  "Contains a function that will stop the server.
  Apply the function and the server will shut down."
  (atom nil))


(def ^:dynamic *request* nil)
(def ^:dynamic *addl-headers* (atom {}))
(def ^:dynamic *doctype* (atom "<!DOCTYPE html>\n"))
(def ^:dynamic *session (atom nil))
(def ^:dynamic *post-processor-funcs* 
  "Add functions to this atom... these functions
  will transform the response... this can be
  stuff like adding cookies and other stuff"
  (atom []))
(def ^:dynamic *response-headers* 
  "Insert any response headers here"
  (atom {}))

(defn new-session 
  "Create a new session container"
  []
  
  {
   :last-seen (atom pmisc/millis), ;; the last time the request was seen
   :request-count (atom 0), ;; the current number of open http requests
   :function-map (atom {}), ;; map GUID strings to functions that will be called when the map
   :handler-channels (atom []) ;; each handler will have a channel... send a message to shut down the handler, if possible
   }
  )

(defn- chan? 
  "Is the parameter a core.async channel?"
  [c]
  (instance? clojure.core.async.impl.channels.ManyToManyChannel c))

(defn url-decode [string]
  "URL Decodes a String"
  (. java.net.URLDecoder decode string "UTF-8"))

(defn url-encode [string]
  "URL Encodes a String"
  (. java.net.URLEncoder encode string "UTF-8`"))

(defn- joiner [x] (clojure.string/join "/" x))


(def suffixs 
  {"html" "text/html",
   "htm"  "text/html",
   "js"   "text/javascript",
   "css"  "text/css",
   "jpg"  "image/jpg",
   "gif"  "image/gif",
   })

(defn- suffixy-thing
  ;; "Given an input, find the suffix"
  [x]
  (cond
    (string? x) (let [lasti (. x lastIndexOf ".")]
                  (--> (if (neg? lasti) x (. x substring (inc lasti)))
                       #(. % toLowerCase)
                       #(get suffixs % "text/plain")))
    (coll? x) (suffixy-thing (last x))
    :else "text/plain"
    ))

(defn- to-response [html path]
  {:status 200
   :headers {"content-type" (suffixy-thing path)}
   :body (cond
           (string? html) (. html getBytes "UTF-8"))})

(defn- build-possible
  [req]
  (filter
    identity
    (mapcat identity
      [[(:ind-path req)]
       (if (and (:path req) (= (count (:suffix req)) 0))
         (let [path (:path req)]
           (map #(conj (butlast path) (str (last path) "." %)) ["html" "htm"])))
       ])))

(defn check-file 
  "Checks to see if the request can be
  returned as a file in the resource directory"
([req]
 (first 
   (filter 
     identity
     (map 
       (fn [path]
         (--> path
              ;; joiner
              #(clojure.string/join "/" %)
              io/resource 
              io/file
              #(if (and (. % exists) (. % isFile)) %)
              file-to-string
              #(to-response % path))) 
       (build-possible req)))))
 ([q req]
  (if (= q :defined?)
    (first 
      (filter
        identity
        (map
          (fn [path]
            (--> path
                 ;; joiner
                 #(clojure.string/join "/" %)
                 io/resource
                 io/file
                 #(and (. % exists) (. % isFile)))) 
          (build-possible req)))))))

(defn path-split [path]
  "Take a path in the form /foo/bar/baz
  and convert it into a Vector of strings and
  then look at the strings and create
  a map with the keys :raw-path which
  is the uri split by slashs and URL decoded,
  :ind-path which has all the empty path
  elements converted into 'index', :suffix
  which contains the suffix of the last path
  element, :end-slash which is a boolean that
  is true if the last path element was empty,
  and :path which is the :ind-path but the
  last element has the suffix removed."
(let 
  [raw-path 
   (into []
         (map 
           url-decode
           (drop 1 (. path split "/"))))
   ind-path
   (--> raw-path
        #(if (empty? %) ["index"] %)
        (fn [q] (into [] (map #(if (zero? (count %)) "index" %) q)))
        )
   [end-body end-suffix]
   (--> (last ind-path)
        #(. % split "\\.")
        #(if (> (count %) 1) [(clojure.string/join "." (butlast %)) (last %)] [(first %) ""]))]
  {:raw-path raw-path
   :ind-path ind-path
   :path (conj (into [] (butlast ind-path)) end-body)
   :suffix end-suffix
   :end-slash (or (empty? raw-path) (zero? (count (last raw-path))))}))

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
    (path-split (:uri r))
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

(def stuff (or-else stuff1 stuff2 check-file))

(defn hello-world [channel _request]
  (let [request (fix_req _request)]
    (go
      (if (stuff :defined? request)
        (loop [res (stuff request)]
          (cond
            (deref? res) (enqueue channel @res)
            (chan? res) 
            (let [tmo (timeout 5000)
                  [c v] (alts! [res tmo])]
              (cond
                (= c tmo) (enqueue channel {:status 408
                                            :headers {"content-type" "text/plain"}
                                            :body "timeout"})
                :else (recur v)))
            
            
            (and 
              (map? res)
              (contains? res :status)
              (contains? res :body)) 
            (enqueue channel res)
            
            :else (enqueue channel
                           {:status 200
                            :headers {"content-type" "text/html"}
                            :body (str res)})))
        (enqueue channel {:status 200
                          :headers {"content-type" "text/plain"}
                          :body (str request)})))))


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
