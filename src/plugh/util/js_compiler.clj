(ns plugh.util.js-compiler
  (:require [cljs.closure :as whole]
            [cljs.compiler :as comp]
            [cljs.analyzer :as ana]
            [cljs.source-map :as sm])
  (:import [java.io PushbackReader BufferedReader StringReader]
           [clojure.lang ISeq]
           [javax.script ScriptEngineManager]
           
           )
  )

(defn thing []
    (let [x (whole/build 
      '[(ns hello.core
          ;; (:require-macros [cljs.core.logic.macros :as m])
          (:use-macros [cljs.core.logic.macros :only [run* conde == defrel fact]])
          (:require [cljs.core.logic :as cl])
;;          (:require [clojure.core.logic :as ccl])
          )
        
        (defn strkey 
          "Helper fn that converts keywords into strings"
          [x]
          (if (keyword? x)
            (name x)
            x))
        
        (extend-type object
          ILookup
          (-lookup
            ([o k]
             (aget o (strkey k)))
            ([o k not-found]
             (let [s (strkey k)]
               (if (goog.object.containsKey o s)
                 (aget o s)
                 not-found)))))

        
        (defn ^{:export greet} greet [n] (str "Hola " n))
        (defn ^:export sum [xs] 42)
        
        (def dog "moo")
        
        (defn sloth [& p] (println "Hi" p))
        
        (+ 1 1)
        
        (defn ee [r] (re-seq
                       #"(?i)7" r))
        
        (def x {})
        
        (def y (conj x {:baz 77}))
        
        (def z (conj y {:foo 99}))
        
        
        (defn ^:export frog [x] (println x))
        
        ;;(cl/defrel points person n) 
        ;;(cl/fact points :bob 2)
        ;;(cl/fact points :charlie 3)
        
        (defn moof []
                 (run* [q]
                         (conde
                           [(== 44 x)]
                           [(== q x)]
                           [(== q 6)]
                           [(== 8 q)])))
                  ;;       (membero q '(:cat :dog :bird :bat :debra))))
        
        ;; (greet (str "it is " (moof)))
        
        (defn ^{:export runit} run-it [a1 b1 c1]
            (clj->js
              (let [a (js->clj a1)
                  b (js->clj b1)
                  c (js->clj c1)]
                
                ;; (mapcat #(if (or (= nil %) (js/isNaN %)) [] [%]) [(. js/Date parse "Jqewqwewqeun 19, 2013 00:00:00 +0000")])
                [1]
              ))
          )
        
        ]
      ;; {:optimizations :advanced :pretty-print true}
      {:optimizations :simple :pretty-print true}
      )]
      
      ;; (println "script " x)
      x
    ))

(comment 
(defn execute-in-context 
  "Execute a string in a context"
  [string]
  (let [context (. Context enter)
        scope (. context initStandardObjects)]
    (. context setOptimizationLevel -1)
    (try
    (. context evaluateString scope, string "Lift"  0 nil) 
    (finally (. Context exit)))))

(defn that []
  (let [src (str "function (__p1, __p2, __p3) {" (thing) "\n\n return runit(__p1, __p2, __p3);}")]
    (println "js sez " (execute-in-context (str src "(1)")))))
)