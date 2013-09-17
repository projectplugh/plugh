(ns plugh.http.server
  (:gen-class)
  (:use plugh.util.file
        clojure.core.async)
  (:require 
    [plugh.util.misc :as pm]
    [plugh.util.js-compiler :as jc]
    [clojure.java.io :as io]
    [org.httpkit.server :as hs]
    [clojure.edn :as edn]
    ))



(def server-stopper 
  "Contains a function that will stop the server.
  Apply the function and the server will shut down."
  (atom nil))


(def ^:dynamic *websocket* nil)

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
   :last-seen (atom pm/millis), ;; the last time the request was seen
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
                  (some-> (if (neg? lasti) x (. x substring (inc lasti)))
                       (#(. % toLowerCase))
                       (#(get suffixs % "text/plain"))))
    (coll? x) (suffixy-thing (last x))
    :else "text/plain"
    ))

(defn- to-response [html path]
  {:status 200
   :headers {"content-type" (suffixy-thing path)}
   :body (str html)})

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

(defn- build-path [path]
  (let [looker (fn [base] (some-> (into [base] path)
              ;; joiner
              (#(clojure.string/join "/" %))
              io/resource
              io/file
              (#(if (and (. % exists) (. % isFile)) %))))]
    (first (filter identity (map looker '("static" "target"))))))

(defn check-file 
  "Checks to see if the request can be
  returned as a file in the resource directory"
([req]
 (first 
   (filter 
     identity
     (map 
       (fn [path]
         (some-> (build-path path)
              file-to-string
              (#(to-response % path))))
       (build-possible req)))))
 ([q req]
  (if (= q :defined?)
    (first 
      (filter
        identity
        (map build-path
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
   (some-> raw-path
        (#(if (empty? %) ["index"] %))
        ((fn [q] (into [] (map #(if (zero? (count %)) "index" %) q))))
        )
   [end-body end-suffix]
   (some-> (last ind-path)
        (#(. % split "\\."))
        (#(if (> (count %) 1) [(clojure.string/join "." (butlast %)) (last %)] [(first %) ""])))]
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
  (pm/match-pfunc
    [{:path [(:or "cat" "sloth") & rest]
      :query q}]
    {:status 200
     :headers {"content-type" "text/html"}
     :body (str "cat: " rest " query " q)}))

(def stuff2
  (pm/match-pfunc
    [{:path [(:or "moose" "sloth") & rest]
      :query q}]
    {:status 200
     :headers {"content-type" "text/html"}
     :body (str "moose: " rest " query " q)}))

(def start-app
  (pm/match-pfunc
    [{:path ["plugh" "boot"]
      :suffix "js"
      :query q}]
    {:status 200
     :headers {"content-type" "application/javascript"}
     :body "console.log('hi');"}))

(def stuff (pm/or-else start-app stuff1 stuff2 check-file))

(declare find-chan-for-guid)
(declare find-guid-for-chan)

(defmethod print-method clojure.core.async.impl.channels.ManyToManyChannel [chan, ^java.io.Writer w]
   (let [guid (find-guid-for-chan chan)]
  (.write w "#guid-chan\"")
  (.write w guid)
  (.write w "\"")))

(defn ping-channel [ch]
  (hs/on-close ch (fn [status] (println "Channel " ch " closed")))
  (hs/on-receive ch (fn [data] 
                      (binding [*websocket* ch]
                        (let [thing (edn/read-string {:readers {'guid-chan find-chan-for-guid}} data)
                              chan (:chan thing)
                              msg (:msg thing)]
                          (if (and chan msg)
                            (go (>! chan msg))))))))

(def base (atom (+ 1000000000000000 (long (rand 1000000000000000)))))


(defn mguid 
  "Make a GUID"
  []
  (str "G" (swap! base inc) "Z" (long (rand 100000000000)))
  )



(def chan-to-guid (atom {}))

(def guid-to-chan (atom {}))

(defn register-chan [chan guid]
  (swap! chan-to-guid assoc chan guid)
  (swap! guid-to-chan assoc guid chan))

(defn find-guid-for-chan [chan]
  (or (get @chan-to-guid chan)
    (let [guid (mguid)]
      (register-chan chan guid)
      guid)))

(defn find-chan-for-guid [guid]
  (or (get @guid-to-chan guid)
      (let [nc (chan)
            socket *websocket*]
        (register-chan nc guid)
        (go
          (loop []
            (let [msg (<! nc)]
              (hs/send! socket {:body (pr-str {:chan nc :msg msg})})
              )
            (recur)
            )
          )
        nc)
      ))

(defn make-server-chan [name]
 (let [ch (chan)]
   (register-chan ch name)
   ch)
  )

(def listeners (atom []))

(def chats (atom []))


(defn req-handler [_request]
  (hs/with-channel 
    _request
    channel 
    (let [request (fix_req _request)]
      (if (hs/websocket? channel)  (do (ping-channel channel)))
      (go
        (if (stuff :defined? request)
          (loop [res (stuff request)]
            (cond
              (pm/deref? res) (hs/send! channel @res)
              (chan? res) 
              (let [tmo (timeout 5000)
                    [c v] (alts! [res tmo])]
                (cond
                  (= c tmo) (hs/send! channel {:status 408
                                            :headers {"content-type" "text/plain"}
                                            :body "timeout"})
                  :else (recur v)))
              
              
              (and 
                (map? res)
                (contains? res :status)
                (contains? res :body)) 
              (hs/send! channel res)
              
              :else (hs/send! channel
                           {:status 200
                            :headers {"content-type" "text/html"}
                            :body (str res)})))
          (hs/send! channel {:status 200
                           :headers {"content-type" "text/plain"}
                           :body "{}"}))))))



(defn start-server [port]
  (println "Running server on " port)
  (go 
    (let [server-chan (make-server-chan "The Chat Server")]
      (while true
        (let [v (<! server-chan)]
          (when-let [n (:add v)]
            (swap! listeners #(conj % n))
            (>! n @chats))
          
          (when-let [chat-msg (:msg v)]
            (swap! chats #(conj % chat-msg))
            (doseq [ch @listeners] (>! ch [chat-msg])))
          ))))
  
  (go
    (let [compiler-chan (make-server-chan "cljs compiler")]
      (while true
        (let [v (<! compiler-chan)]
          (when-let [from (:from v)]
            (let [res (try (jc/compile-string (:source v)) (catch Exception e (do (println "we gots an exception " e) e)))
                  to-send (if (instance? Exception res) {:org v :error (str res)} {:org v :result res})]
              (>! from to-send)))))))

  (reset! server-stopper (hs/run-server req-handler {:port port})))
  

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
