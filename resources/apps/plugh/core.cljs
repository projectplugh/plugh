(ns plugh.core
  (:require-macros
    [cljs.core.async.macros :as m :refer [go alt!]]
    [clang.angular :refer [def.controller defn.scope def.filter fnj]])
  (:use [clang.util :only [? module]])
  (:require
            [cljs.reader :as cr]
            [cljs.core.async :as async
             :refer [<! >! chan close! sliding-buffer put! alts!]]))



(def m (module "plugh.app" ["clang", "ngGrid"]))

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

(defn forward-msg [nc guid]
  (go
      (let [msg (<! nc)]
            (send-to-server (pr-str {:chan nc :msg msg}))
            (forward-msg nc guid))))

(defn find-chan-for-guid [guid]
  (or (get @guid-to-chan guid)
      (let [nc (chan)]
        (register-chan nc guid)
        (forward-msg nc guid)
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
  (let [c @comm
        ms msg]
    (if (= 1 (.-readyState c))
      (do
        (.send c ms))
      (go
        (let [to (<! (async/timeout 50))]
          (send-to-server msg)
          )))))

(defn to-num [x] 
  (cond 
    (number? x) x
    (string? x) (js/parseFloat x)
    :else 0))

(defn sum [x]
  (let [mapped (map to-num x)
        ret (reduce + 0 mapped)]
  ret
  ))

(defn avg [x]
  (if (empty? x) 0 (/ sum (count x))))
