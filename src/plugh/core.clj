(ns plugh.core
  (:use plugh.http.server)
  (:require [clojurewerkz.welle.core    :as wc]
            [clojurewerkz.welle.buckets :as wb]
            [clojurewerkz.welle.kv      :as kv])
  (:import com.basho.riak.client.http.util.Constants)
  (:gen-class )
  )

(defn put-in-riak [key it n]
  (io!
    (println "Storing " key " value " it " n " n " clz " (.getClass (long n)))
    (kv/store "thing" key it :content-type Constants/CTYPE_TEXT_UTF8 :indexes {:sage [(long n)]})
    ))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "dog")
 ;; (wc/connect!)
;;  (wb/create "thing")
  ;; (run-server 8080)
  ;;(dorun (map #(put-in-riak (str "k" %) (str "hi dude " %) %) (repeatedly 10000 (fn [] (rand-int 19299999)))))
  ;;(println "Answer " (count (sort (into () (kv/index-query "thing" :sage [1 200000000])))))
  (println "Hello, World!"))
