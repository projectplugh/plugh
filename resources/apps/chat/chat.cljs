(ns demo.chat
  (:require-macros
    [cljs.core.async.macros :as m :refer [go]]
    [clang.angular :refer [def.controller defn.scope def.filter in-scope s-set fnj]])
  (:use [clang.util :only [? module]])
  (:require [plugh.core :as pc]
            [cljs.core.async :as async
             :refer [<! >! chan]]))

(def server-chan (pc/server-chan "The Chat Server"))

(def compiler-chan (pc/server-chan "cljs compiler"))

(def.controller pc/m Chatter [$scope $compile]
  (s-set :chats (clj->js []))
  
  (s-set :line "")
  
  (defn.scope send [] 
    (let [msg (:line $scope)]
      (go 
        (>! server-chan {:msg msg})))
    (assoc! $scope :line ""))
  
  
  (let [rc (chan)]
    (go (>! server-chan {:add rc}))
    (letfn [(proc [] 
                  (go (let [chats (<! rc)]
                        (in-scope (doseq [m chats] (.push (:chats $scope) m)))
                        (proc))))]
      (proc)))
  )
    
    