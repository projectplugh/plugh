(ns plugh.visi.hack
  (:require [clojure.core.async :as async :refer [mult tap map< go chan timeout]]
            ))

(def a (chan))

(def a-m (mult a))

(def res (map< #(+ % 1) (tap a-m (chan))))

(def res-m (mult res))

(println "Woof")

(let [c (tap res-m (chan))]
  (async/go-loop [] (do (println "Hi res is " (async/<! c)) (recur))))

(let [c (tap res-m (chan))]
  (async/go-loop [] (do (println "Hi3 res is " (async/<! c)) (recur))))