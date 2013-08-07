(ns plugh.core
  (:use 
    plugh.util.file
    plugh.util.misc
   
    
    clojure.core.async)
  (:require 
    [plugh.util.js-compiler :as jsc]
    [plugh.http.server :as ps]
    [ plugh.html.parser :as wonky1]
    [plugh.http.javascript :as wonky2])
  (:gen-class )
  )

(comment 
(defn put-in-riak [key it n]
  (io!
    (println "Storing " key " value " it " n " n " clz " (.getClass (long n)))
    (kv/store "thing" key it :content-type Constants/CTYPE_TEXT_UTF8 :indexes {:sage [(long n)]})
    ))


(defn date-from-line [s]
  "Pulls a date from a line and converts it to a millis"
  (let [f (comp second first)
        str (f (re-seq #"\[(.*?)\]" s))
        millis (. (. DateTimeFormat forPattern "dd/MMM/YYYY:HH:mm:ss Z") parseMillis str)
        ]
    millis
    ))

(defn load-for [service] 
  (wc/connect!)
  (wb/create service)
  (let [root (str "/home/dpp/storage/logs/" service)]
    (map-file root 
              (fn [file] 
                (cond 
                  (. (. file getName) endsWith "request.log")
                  (do 
                    (map-file-lines 
                      file 
                      (fn [line]
                        (let [millis (date-from-line line)]
                          (kv/store 
                            service
                            (str "k" (rand-int (Integer/MAX_VALUE))) 
                            line :content-type Constants/CTYPE_TEXT_UTF8 
                            :indexes {:when [(long millis)]
                                      :service [service]
                                      :type ["jetty"]})
                          (println "Wrote " millis)
                          )
                        )))
                  )))
    ))

(defn mr-job [service]
  (let [src "function (a, b, c) {return [1];}"] ;; (str "\n\n function(__p1, __p2, __p3) {\n\n" (jsc/thing) "\n\nreturn runit(__p1, __p2, __p3);}\n\n")]
    (mr/map-reduce 
      {:inputs service,
       :timeout 9000000,
       :query 
       [{:map {:language "javascript"
               :source src}}
        {:reduce {:language "javascript"
                  :source "function (red) {
                          var sum = 0;
                          for (var x = 0; x < red.length; x++) {
                          sum = sum + red[x];
                          }
                          
                          return [sum];
                          
                          }"
}}
        ]}))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "dog")
  ;(println "It's "(map-file-lines (first args) #(.length %)))
  ;(wc/connect!)
  (println "mmmooo")
  ;(wb/create "thing")
  (println "ox")
  ;; (run-server 8080)
  ;(dorun (map #(put-in-riak (str "k" %) (str "hi dude " %) %) (repeatedly 10000 (fn [] (rand-int 19299999)))))
  ; (dorun (load-for "seventhings"))
  ; (jsc/thing)
  ; (println "The result of the MR job is " (mr-job "seventhings"))
  ;;(println "Answer " (count (sort (into () (kv/index-query "thing" :sage [1 200000000])))))
  (println "Hello, World!"))

(defn t3 []
  (loop [x 1]
    (cond
      (< x 5) (recur (inc x))
      (< x 20) (recur (+ 88 x))
      :else x
      ))
  )

(defn t2 []
  (let [c (chan)]
    (go
      (loop [v 0]
        (let [val (<! c)] 
          (if (not val) 
            (println "exit with v " v) 
            (do 
              (print (str "val " val " v " v "\n"))
              (recur (inc v)))))))
    
    (go
      (loop [v 0]
        (let [val (<! c)] 
          (if (not val) 
            (println "exit 2 with v " v) 
            (do 
              (print (str "2: val " val " v " v "\n"))
              (recur (inc v)))))))
    
    
    (dotimes [i 50] (>!! c (str "hi " i)))
    (dotimes [i 50] (>!! c (str "hi again " i)))
    
    (close! c)
    
    )
  "dine")
(comment 
(defn thingy [] 
  (let [name "/home/dpp/logs/seventhings/logs/2013_05_24.request.log"
        str (first (map-file-lines name identity))]
    (date-from-line str)
    (let [it (map-file-lines name (fn [x] [x (date-from-line x)]))]
      [(count it) (first it)])))

(defn meow [] "woof")
)
