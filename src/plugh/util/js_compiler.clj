(ns plugh.util.js-compiler
  (:require [cljs.closure :as whole])
  (:import [java.io PushbackReader BufferedReader StringReader]
           [clojure.lang ISeq]
           [javax.script ScriptEngineManager]
           )
  )


(defn thing []
  (println 
    (whole/build 
      '[(ns hello.core)
        (comment 
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
        )
        
        (defn ^:export frog [x] (println x))
        
        
        ]
      {:optimizations :simple :pretty-print true})
    ))
