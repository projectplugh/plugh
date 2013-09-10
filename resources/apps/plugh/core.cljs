(ns plugh.core
  (:use-macros 
    [cljs.core.logic.macros :only [run* conde == defrel fact]])
  (:require-macros
    [cljs.core.async.macros :as m :refer [go alt!]]
    [clang.angular :refer [def.controller defn.scope def.filter fnj]])
  (:use [clang.util :only [? module]])
  (:require [cljs.core.logic :as cl]
            [cljs.reader :as cr]
            [cljs.core.async :as async
             :refer [<! >! chan close! sliding-buffer put! alts!]]))



(def m (module "plugh.app" ["clang"]))

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
  (if (contains? @chan-to-guid chan)
    (get @chan-to-guid chan)
    (let [guid (mguid)]
      (register-chan chan guid)
      guid)))

(declare send-to-server)


(defn find-chan-for-guid [guid]
  (or (get @guid-to-chan guid)
      (let [nc (chan)]
        (register-chan nc guid)
        (go
          (let [run (atom true)]
          (while @run
            (let [msg (<! nc)]
              (if msg
              (send-to-server (pr-str {:chan nc :msg msg}))
              (swap! run false)
              )))))
        nc)
      ))

(extend-protocol IPrintWithWriter
  cljs.core.async.impl.channels/ManyToManyChannel
  (-pr-writer [chan writer opts]
              (let [guid (find-guid-for-chan chan)]
              (-write writer "#guid-chan\"")
              (-write writer guid)
              (-write writer "\""))))


(defn ^:private guid-chan-reader
  [s]
  (find-chan-for-guid (str s)))

(cr/register-tag-parser! "guid-chan" guid-chan-reader)

(defn server-chan [name]
  (find-chan-for-guid (str name))
  )


(declare comm)

(defn setup-comm []
  (let [ret (new js/WebSocket "ws://localhost:9898/comet")]
    (set! (.-onmessage ret) (fn [me] 
                              (do
                                (let [info (.-data me)
                                      parsed (cr/read-string info)
                                      chan (:chan parsed)
                                      msg (:msg parsed)]
                                  (if (and chan msg)
                                    (do 
                                      (go (>! chan msg)))                             
                                    )))))

    (set! (.-onopen ret) (fn [me] 
                           (.log js/console (str "socket open " (.stringify js/JSON me)))))
    (set! (.-onclose ret) (fn [me]
                            (set! comm (setup-comm))
                             (.log js/console (str "closed " (.stringify js/JSON me)))))
    ret))

(def comm (atom (setup-comm)))

(defn send-to-server [msg] 
  (go
    (loop []
      (let [c @comm
            ms msg]
        (if (= 1 (.-readyState c))
          (.send c ms)
          (let [to (<! (async/timeout 50))]
            (recur))
          )))))

