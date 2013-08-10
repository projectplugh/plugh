(ns demo.chat
  (:require-macros
    [cljs.core.async.macros :as m :refer [go alt!]]
    [clang.angular :refer [def.controller defn.scope def.filter in-scope fnj]])
  (:use [clang.util :only [? module]])
  (:require [plugh.core :as pc]
            [cljs.core.async :as async
             :refer [<! >! chan close! sliding-buffer put! alts!]]))

(def listeners (atom []))

(def chats (atom []))

(def server-chan (chan))

(go 
  (while true
    (let 
      [v (<! server-chan)]
      (cond
        (:add v) 
        (let [nc (:add v)]
          (swap! listeners #(conj % nc))
          (>! nc @chats))
        (not (empty? (:msg v)))
        (let [m (:msg v)]
          (swap! chats #(conj % m))
          (doseq [ch @listeners] (>! ch [m]) )
          )
        :else nil)
      )))

(def.controller pc/m Chatter [$scope]
  (assoc! $scope :chats (clj->js []))
  
  (assoc! $scope :line "")
  
  (defn.scope send [] 
    (let [msg (:line $scope)]
      (go 
        (>! server-chan {:msg msg})))
    (assoc! $scope :line ""))
  
  (go
    (let [rc (chan)]
      (>! server-chan {:add rc})
      (while true
        (let [chats (<! rc)]
          (in-scope (doseq [m chats] (.push (:chats $scope) m)))
        ))))
  )
    
    