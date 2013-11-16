(ns plugh.visi.parser
  (:require [instaparse.core :as insta]
            [clojure.core.async :as async :refer [mult]]
            [plugh.util.misc :as misc]))


(def parse-def
  "
  Lines = (Line (<'\n'>)*)*
  Line = ((BlockComment <'\n'>) | LineComment)* (SINK / Def / Source)
  <Def> = <START_OF_LINE> (ConstDef | FuncDef);
  ConstDef = IDENTIFIER SPACES <'='> SPACES? EXPRESSION SPACES? <'\n'>;
  FuncDef = IDENTIFIER SPACES? <'('> SPACES? (IDENTIFIER SPACES? <','> SPACES?)* IDENTIFIER SPACES? <','>? SPACES? <')'> SPACES? 
            <'='> EXPRESSION SPACES? <'\n'>;
  SINK = <START_OF_LINE> <'sink:'> SPACES? IDENTIFIER SPACES? <'='> EXPRESSION <'\n'>;
  Source = <START_OF_LINE> <'source:'> SPACES? IDENTIFIER SPACES? <'\n'>;
  START_OF_LINE = #'^' ;
  <SPACES> = (<'\n'> SPACES) / (SPACES <'\n'> SPACES) / (SPACES? LineComment SPACES?) / (SPACES? BlockComment SPACES?)  / <(' ' | '\t')+>
  LineComment = (SPACES? <';;'> (#'[^\n]')*) 
  BlockComment = <'/*'> (BlockComment | (!'*/' AnyChar))* <'*/'>
  <AnyChar> = #'.' | '\n'
  IDENTIFIER = #'[a-zA-Z][a-zA-Z0-9\\-_\\?]*'
  ClojureSymbol = #'([a-zA-Z\\-\\*\\+\\!\\_][a-zA-Z0-9\\-\\.\\*\\+\\!\\_]*)\\/[a-zA-Z\\-\\*\\+\\!\\_][a-zA-Z0-9\\-\\.\\*\\+\\!\\_]*'
  BlockExpression = SPACES? <'begin'> SPACES (EXPRESSION)+ SPACES <'end'> 
  EXPRESSION = (BlockExpression | (SPACES (ConstDef | FuncDef) SPACES EXPRESSION) |
               IfElseExpr | FuncCall | ParenExpr |  ConstExpr |
               FieldExpr | FunctionExpr | MapExpr | VectorExpr |
               (SPACES? (IDENTIFIER | ClojureSymbol) SPACES?)) (SPACES Operator SPACES EXPRESSION SPACES?)?
  FuncCall = SPACES? (IDENTIFIER | ClojureSymbol) SPACES? <'('> (EXPRESSION <','>)*  EXPRESSION <','>? SPACES? <')'> SPACES?
  ParenExpr = (SPACES? <'('> SPACES Operator SPACES <')'> SPACES?) |
              (SPACES? <'('> SPACES? EXPRESSION SPACES Operator SPACES <')'> SPACES?) |
              (SPACES? <'('> SPACES Operator SPACES EXPRESSION SPACES? <')'> SPACES?) |
              (SPACES? <'('> EXPRESSION <')'> SPACES?)
  Keyword = <':'> IDENTIFIER 
  IfElseExpr = SPACES? <'if'> SPACES EXPRESSION SPACES <'then'> SPACES EXPRESSION SPACES <'else'> SPACES EXPRESSION
  ConstExpr = SPACES? (Number | Keyword | StringLit) SPACES?
  FieldExpr = SPACES? IDENTIFIER? (SPACES? '.' IDENTIFIER)+ SPACES?
  FunctionExpr = SPACES? (IDENTIFIER | (<'('> SPACES? (IDENTIFIER SPACES? <','> SPACES?)*  IDENTIFIER SPACES? <','>? SPACES? <')'> ) ) SPACES? <'=>'> SPACES? EXPRESSION SPACES?
  Number = #'(\\-|\\+)?\\d+' NumberQualifier?
  NumberQualifier = ('%' | ':minutes' | ':hours' | ':seconds' | ':days')
  MapExpr = SPACES? <'{'> (Pair <','>)* Pair (<','> SPACES?)? <'}'> SPACES?
  VectorExpr = SPACES? <'['> (EXPRESSION <','>)* EXPRESSION (<','> SPACES?)? <']'> SPACES?
  Pair = EXPRESSION <'->'> EXPRESSION
  StringLit = <'\"'> ('\\\"' / #'[^\"]')* <'\"'>
  Operator = '+' | '-' | '*' | '/' | '&' | '>' | '<' | '==' | '>=' | '<=' | '&&' | '||' | '<>' | '>>' | ':=' | ':>='
  " )

(def operator-map {":=" 'plugh.util.misc/arrow-assignment,
                   "||" 'or
                   ":>=" 'plugh.util.sent-analysis/arrow-update-func})

(defn fix-op [op] (do 
                    (or (get operator-map (str op)) op)))

(defn- replace-in-i [what the-map]
  (cond
    (and (symbol? what) (= \# (last (name what))) (= \$ (first (name what))))
    (get the-map what)
    (and (symbol? what) (= \# (last (name what))))
    [(get the-map what)]
    (vector? what) [(into [] (mapcat #(replace-in-i % the-map) what))]
    (map? what) [(into {} (map (fn [[k v]] [(replace-in-i k the-map) (replace-in-i v the-map)]) what))]
    (seq? what) [(doall (mapcat #(replace-in-i % the-map) what))]
    :else [what]
    ))

(defn replace-in [what the-map] (doall (first (replace-in-i what the-map))))

(def the-parser
  (insta/parser parse-def
    :start :Lines ))

(defn visi-parse [s]
  (let [sp (if (.endsWith s "\n") s (str s "\n"))]
    (the-parser sp))
  )


(defn dollarize-name [name] (str name "$mult"))

(defn first-is [coll test]
  (and (sequential? coll) (= test (first coll))))

(defn filter-comments [exp]
  (filter (fn [q] (not (or (first-is q :BlockComment )
                         (first-is q :LineComment )))) exp))

(defn flatten-exp [exp]
  (if (< (count exp) 3) exp
    (let [t1 (take 2 exp)
          t2 (drop 2 exp)
          flattened
          (mapcat
            (fn [q]
              (do
                (if (first-is q :EXPRESSION )
                  (do
                    (flatten-exp (rest q)))
                  [q]))) t2)
          final (into (into [] t1) flattened)]
      final
      )))

(defn exp-to-clj [exp-a set-of-chans ref-chan-atom]
  (let [exp (filter-comments exp-a)]
    (cond
      (first-is exp :EXPRESSION )
      (exp-to-clj (rest exp) set-of-chans ref-chan-atom)

      (first-is exp :IDENTIFIER)
      (let [the-id (second exp)]
        (if (contains? set-of-chans the-id)
          (do
            (swap! ref-chan-atom #(conj % the-id))
            (symbol (str the-id "$")))
          (symbol the-id)))

      (first-is exp :Passthru)
      (second exp)

      (and (= (count exp) 1) (first-is (first exp) :Passthru))
      (second (first exp))


      (first-is exp :FunctionExpr)
      (let [[params body]
            (split-with #(= :IDENTIFIER (first %)) (rest exp))]
              (replace-in '(fn [$params#] body#)
                {
                  '$params# (map #(-> % second symbol) params)
                  'body# (exp-to-clj body  (apply disj set-of-chans (map #(-> % second) params)) ref-chan-atom)
                }
              ))

      (first-is exp :MapExpr)
      (let [pairs (map (fn [pair] (into [] (map #(exp-to-clj %  set-of-chans ref-chan-atom) (rest pair)))) (rest exp))]
        (into {} pairs))

      (first-is exp :VectorExpr)
      (into [] (map #(exp-to-clj %  set-of-chans ref-chan-atom) (rest exp)))

      (and (= 1 (count exp)) (first-is (first exp) :ParenExpr))
      (exp-to-clj (first exp)  set-of-chans ref-chan-atom)

      (and (first-is exp :ParenExpr ) (= 2 (count exp)))
      (exp-to-clj (second exp)  set-of-chans ref-chan-atom)

      (and (first-is exp :ParenExpr ) (= 4 (count exp)))
      (exp-to-clj (rest exp)  set-of-chans ref-chan-atom)

      (and (first-is exp :ParenExpr ) (first-is (second exp) :Operator))
      (let [var-name (misc/mguid)
            q (conj (rest exp) [:IDENTIFIER var-name])
            body (exp-to-clj q  set-of-chans ref-chan-atom)]
        (replace-in '(fn [var-name#] body#)
          {'var-name# (symbol var-name)
           'body# body}))

      (first-is exp :ParenExpr)
      (let [var-name (misc/mguid)
            q (reverse (conj (reverse (rest exp)) [:IDENTIFIER var-name]))
            body (exp-to-clj q  set-of-chans ref-chan-atom)]
        (replace-in '(fn [var-name#] body#)
          {'var-name# (symbol var-name)
           'body# body}))

      (and (sequential? exp) (first-is (first exp) :ConstDef))
      (let [cd (first exp)
            body (rest exp)]
      (replace-in '(let [var-name# var-def#] body#)
        {'var-name# (symbol (second (second cd)))
         'var-def# (exp-to-clj (drop 2 cd)  set-of-chans ref-chan-atom)
         'body# (exp-to-clj body  set-of-chans ref-chan-atom)}))

      (and (sequential? exp) (first-is (first exp) :FuncDef))
      (let [cd (first exp)
            name (symbol (second (second cd)))
            params (take-while #(first-is % :IDENTIFIER) (drop 2 cd))
            func-def (drop-while #(first-is % :IDENTIFIER) (drop 2 cd))
            body (rest exp)]
        (replace-in '(letfn [(func-name# [$func-param#] func-def#)] body#)
          {'func-name# name
           '$func-param# (map #(-> % second symbol) params )
           'func-def# (exp-to-clj func-def (apply disj set-of-chans (map #(-> % second) params)) ref-chan-atom)
           'body# (exp-to-clj body set-of-chans ref-chan-atom)}))

      (and (first-is (second exp) :Operator ) (= ">>" (second (second exp))))
      (let [left (first exp)
            parts (partition 2 (rest (flatten-exp exp)))
            [threads others] (split-with #(= ">>" (second (first %))) parts)
            intermediate (into [:EXPRESSION [:Passthru (replace-in '(plugh.util.misc/--> first# $rest#)
                                                         {
                                                           'first# (exp-to-clj left  set-of-chans ref-chan-atom)
                                                           '$rest# (map #(-> % second ((fn [q] (exp-to-clj q  set-of-chans ref-chan-atom)))) threads)
                                                           }
                                                         )
                                             ]] (mapcat identity others))
            ]
        (exp-to-clj intermediate set-of-chans ref-chan-atom))

      (first-is (second exp) :Operator )
      (replace-in '(op# left# right#)
        {'op# (fix-op (symbol (second (second exp))))
         'left# (exp-to-clj (take 1 exp) set-of-chans ref-chan-atom)
         'right# (exp-to-clj (drop 2 exp) set-of-chans ref-chan-atom)})

      (first-is (first exp) :IDENTIFIER )
      (let [the-id (second (first exp))]
        (if (contains? set-of-chans the-id)
          (do
            (swap! ref-chan-atom #(conj % the-id))
            (symbol (str the-id "$")))
          (symbol the-id)))

      (first-is (first exp) :ConstExpr )
      (exp-to-clj (second (first exp)) set-of-chans ref-chan-atom)

      (and (first-is (first exp) :Number )  (= 2 (count (first exp))))
      (do
        (read-string (second (first exp))))

      (and (first-is (first exp) :Number ))
      (do
        (let [number (read-string (second (first exp)))]
        (condp = (second (nth (first exp) 2))
          "%" (/ number 100)
          ":minutes" (* number 1000 60)
          ":hours" (* number 1000 60 60)
          ":seconds" (* number 1000)
          ":days" (* number 1000 60 60 24))))

      (and (first-is exp :Number ) (= 2 (count exp)))
      (read-string (second exp))

      (and (first-is exp :Number ))
      (do
        (let [number (read-string (second  exp))]
          (condp = (second (nth exp 2))
            "%" (/ number 100)
            ":minutes" (* number 1000 60)
            ":hours" (* number 1000 60 60)
            ":seconds" (* number 1000)
            ":days" (* number 1000 60 60 24)
            )))


      (first-is exp :FuncCall )
      (replace-in '(op# $params#)
        {'op# (symbol (second (second exp)))
         '$params# (map #(exp-to-clj %  set-of-chans ref-chan-atom) (drop 2 exp))
         })

      (and (first-is exp :FieldExpr ) (= "." (second exp)) (= 3 (count exp)))
      (keyword (second (nth exp 2)))

      (and (first-is exp :FieldExpr ) (= "." (second exp)))
      (let [var-name (misc/mguid)]
        (replace-in '(fn [var-name#] (plugh.util.misc/--> var-name# $fields#))
          {'var-name# var-name
           '$fields# (map #(-> % second keyword) (filter #(not (= "." %)) (rest exp)))
           }
          ))

      (first-is exp :FieldExpr )
      (replace-in '(plugh.util.misc/--> var# $fields#)
        {'var# (symbol (second (second exp)))
         '$fields# (map #(-> % second keyword) (filter #(not (= "." %)) (drop 2 exp)))
         })




      (first-is exp :StringLit )
      (clojure.string/join (map (fn [s] (if (and (string? s) (> (count s) 1)) (.substring s 1) s)) (rest exp)))

      (and (= 1 (count exp)) (vector? (first exp)))
      (exp-to-clj (first exp) set-of-chans ref-chan-atom)

      :else (do
              (/ 1 0);; (println "Failed for " exp)
              exp
              )
      )))

(defn maybe-doall [x]
  (if (seq? x) (doall x) x))

(defn build-body [parsed set-of-chans]
   (let [[identifiers body] (split-with #(= :IDENTIFIER (first %)) (rest parsed)) ;; FIXME shadowing
        the-name (second (first identifiers))
        params (map second (rest identifiers))
        referenced (atom [])
        built-body (reduce
                     (fn [expr the-var]
                       (doall (replace-in '(let [target-var# (tap (deref source-var#) (chan 2))]
                                            body#
                                            )
                                          {'body# expr,
                                           'target-var# (symbol (str the-var "$")),
                                           'source-var# (symbol (dollarize-name the-var))}
                                          )))
                     (maybe-doall (exp-to-clj (first body) set-of-chans referenced))
                     @referenced)
        ]
     [the-name params built-body])
  )

(defn ++ [& rest]
  (seq (reduce into [] rest)))

(def chan-def-proto '(def name# (delay body#)))

(def chan-mult-def-proto '(def name-mult# (delay (mult (deref name#)))))

(defn build-chan-def [parsed set-of-chans]
  (let [[the-name params built-body] (build-body parsed set-of-chans)
        ]
    [(doall (replace-in chan-def-proto {'name# (symbol the-name) 'body# built-body}))
     (doall (replace-in chan-mult-def-proto {'name# (symbol the-name) 'name-mult# (symbol (dollarize-name the-name))}))
     ]
    ))

(def func-def-proto '(defn name# [$params#] body#))


(defn build-func-def [parsed set-of-chans]
  (let [[the-name params built-body] (build-body parsed set-of-chans)]
    [(doall (replace-in func-def-proto {'name# (symbol the-name) '$params# (map symbol params) 'body# built-body}))]
    ))



(defn build-header [namespace for-cljs]
  (if for-cljs 
    (replace-in '(ns name#
                  (:require-macros [cljs.core.async.macros :as async]
                                   [plugh.util.misc :as pl-macro])
                  (:require [cljs.core.async :as async :refer [mult tap map< go chan timeout]]
                            [plugh.util.sent-analysis :as analisys :refer [calc-sentiment xform xfilter flow]]))
                {'name# (symbol namespace)})
    (replace-in '(ns name#
                  (:use [plugh.util.sent-analysis])
                  (:require [clojure.core.async :as async :refer [mult tap map< go chan timeout]]
                            [plugh.util.misc :as pl-macro :refer [--> arrow-assignment]]))
                {'name# (symbol namespace)})))

(defn make-source [name]
  (seq (replace-in '[(def name# (chan 2))
                     (def namedollar# (delay (mult name#)))]
             {'name# (symbol name)
              'namedollar# (symbol (dollarize-name name))}
             )))

(defn build-sink-def [x all-chans]
  (let [[the-name params built-body] (build-body x all-chans)
        ]
  (replace-in '(def name# (delay (mult body#)))
              {'name# (symbol the-name)
               'body# built-body}
              )
  ))

(defn visi-compile [ast name-space for-cljs]
  (let [lines (mapcat (fn [q] (filter (fn [ln] (not (or (= :Line ln)
                                                        (and (sequential? ln) (= :BlockComment (first ln)))
                                                        (and (sequential? ln) (= :LineComment (first ln))))))
                                      q))
                      (filter (fn [q] (and (sequential? q) (= :Line (first q)))) ast))
        sources (into #{} (map (fn [x] (second (second x))) (filter #(= :Source (first %)) lines)))
        def-names (into #{} (map (fn [x] (second (second x))) (filter #(= :ConstDef (first %)) lines)))
        all-chans (into sources def-names)
        sinks (into {} (map (fn [x] [(second (second x)) (build-sink-def x all-chans)]) (filter #(= :SINK (first %)) lines)))
        defs (into {} (map (fn [x] [(second (second x)) (build-chan-def x all-chans)]) (filter #(= :ConstDef (first %)) lines)))
        funcs (into {} (map (fn [x] [(second (second x)) (build-func-def x all-chans)]) (filter #(= :FuncDef (first %)) lines)))
        ]
    
    (let [the-code (++ [(build-header (str name-space) for-cljs)] 
                       [(replace-in '(declare $the-names#)
                                    {'$the-names# (map symbol 
                                                       (++ sources 
                                                           def-names
                                                           (keys defs)))})]
                       (mapcat make-source sources)
                       (mapcat identity (vals funcs))
                       (mapcat identity (vals defs))
                       (vals sinks)
                       )]
      (println "The whole code is " the-code)
      (if (not for-cljs)
        (binding [*ns* (create-ns name-space)] 
          (doall (map (fn [code] 
                        (do 
                          (println "Compiling " code)
                          (eval code))) the-code ))))
      
      {:sources sources :sinks (keys sinks) :name-space name-space :code the-code})
    
    )
  )

;; EOF