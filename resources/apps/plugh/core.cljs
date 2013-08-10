(ns plugh.core
  (:use-macros 
    [cljs.core.logic.macros :only [run* conde == defrel fact]])
  (:require-macros
    [cljs.core.async.macros :as m :refer [go alt!]]
    [clang.angular :refer [def.controller defn.scope def.filter fnj]])
  (:use [clang.util :only [? module]])
  (:require [cljs.core.logic :as cl]
            [cljs.reader :as cr]
            [plugh.sloth :as sl]
            [cljs.core.async :as async
             :refer [<! >! chan close! sliding-buffer put! alts!]]))

(def m (module "plugh.app" ["clang"]))

(def base (atom (+ 1000000000000000 (long (rand 1000000000000000)))))

(defn mguid 
  "Make a GUID"
  []
  (str "G" (swap! base inc) "Z" (long (rand 100000000000)))
  )

(.log js/console "Dude " (str (chan) " " (mguid) ))

(.log js/console "Dude 2 " (str [1 "moose" true {:a 33, :b "meow"} (chan) 3]))

;; (.log js/console "Dude 99 " (meta (vary-meta (chan) #(assoc % :guid "hi"))))

;; (.log js/console "Dude 3 " (cr/read-string (str [1 "moose" true {:a 33, :b "meow"} (chan) 3])))


(defn foo [] (sl/woff "hi"))

(foo)

(declare comm)

(defn setup-comm []
  (let [ret (new js/WebSocket "ws://localhost:9898/comet")]
    (set! (.-onmessage ret) (fn [me] (.log js/console me)))
    (set! (.-onopen ret) (fn [me] 
                           (go 
                             (loop [x 1]
                             (<! (async/timeout 500000))
                             (.send ret (str "howdy #" x))
                             (.log js/console (str "sent: howdy #" x " ready state " (.-readyState ret)))
                             (if (= 1 (.-readyState ret)) (recur (inc x)))
                             ))
                           
                           (.log js/console (str "socket open " (.stringify js/JSON me)))))
    (set! (.-onclose ret) (fn [me]
                            (set! comm (setup-comm))
                             (.log js/console (str "closed " (.stringify js/JSON me)))))
    ret))

(def comm (atom (setup-comm)))

(def.controller m TodoCtrl [$scope]
  (assoc! $scope :todos [{:text "learn angular" :done "yes"}
                         {:text "learn cljs" :done "yes"}
                         {:text "build an app" :done "no"}])
  
  
  (assoc! $scope :meow (atom ""))
  
  (assoc! $scope :nums (atom (range 1 10)))
  
  (assoc! $scope :bool true)
  
  (defn.scope addone [[x _]]
    (+ 1 x))
  
  (defn.scope remaining []
    (->>
      (:todos $scope)
      (map :done)
      (remove #{"yes"})
      count)))


(def.controller m Wombat [$scope]
  
  
  (assoc! $scope :thing "hi, dude")
  
  (assoc! $scope :wombat (clj->js ["foo" "bar" "baz"]))
  
  (defn.scope indexes [x] (range 0 (count x)))
  
  (defn.scope dothing [x] 
    (.log js/console "hi")
    (.log js/console (str "Sending " (:thing $scope)))
    (assoc! $scope :thing "")
    )
  
  (defn.scope dude [x] (str "Hello " x)))