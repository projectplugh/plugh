(ns plugh.core
  (:use-macros 
    [cljs.core.logic.macros :only [run* conde == defrel fact]])
  (:require-macros
    [clang.angular :refer [def.controller defn.scope def.filter fnj]])
  (:use [clang.util :only [? module]])
  (:require [cljs.core.logic :as cl]
            [plugh.sloth :as sl]))

(def m (module "clang.todo" ["clang"]))

(def dog str)

(def cat 33)

(def moose 44)

(dog cat moose)

(.log js/console "Dude")


(defn foo [] (sl/woff "hi"))

(foo)

(defn setup-comm []
  (let [ret (new js/WebSocket "ws://localhost:9898/comet")]
    (set! (.-onmessage ret) (fn [me] (.log js/console me)))
    (set! (.-onopen ret) (fn [me] (.log js/console (str "socket open " (.stringify js/JSON me)))))
    (set! (.-onclose ret) (fn [me] (.log js/console (str "closed " (.stringify js/JSON me)))))
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