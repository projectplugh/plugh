(ns plugh.util.js-compiler
  (:require [cljs.closure :as whole])
  (:import [java.io PushbackReader BufferedReader StringReader]
           [clojure.lang ISeq]
           [javax.script ScriptEngineManager]
           [org.mozilla.javascript Scriptable]
           [org.mozilla.javascript UniqueTag]
           [org.mozilla.javascript Context]
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
        
        (greet (str "it is " (moof)))
        
        ]
      ;;{:optimizations :advanced :pretty-print true}
      {:optimizations :simple :pretty-print true}
      )]
      
      ;; (println "script " x)
      x
    ))

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
  (println "js sez " (execute-in-context (str (thing))))
   ;; "\ngreet({a: 33, b: 'hi'});")))
  (println "hi"))


