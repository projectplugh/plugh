(ns plugh.util.js-compiler
  (:require [cljs.closure :as whole]
            [cljs.compiler :as comp]
            [cljs.analyzer :as ana]
            [cljs.source-map :as sm]
            [clojure.tools.reader :as r])
  (:import [java.io PushbackReader BufferedReader StringReader]
           [clojure.lang ISeq]
           [javax.script ScriptEngineManager]))


; Copyright (c) 2012, 2013 Fogus and Relevance Inc. All rights reserved. The
; use and distribution terms for this software are covered by the Eclipse
; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file COPYING the root of this
; distribution. By using this software in any fashion, you are
; agreeing to be bound by the terms of this license. You must not
; remove this notice, or any other, from this software.

;; from (ns himera.server.setup)

(def ^:private core-names
  '{re-pattern {:name cljs.core.re-pattern},
    keyword? {:name cljs.core.keyword?},
    max-key {:name cljs.core.max-key},
    list* {:name cljs.core.list*},
    == {:name cljs.core.==},
    instance? {:name cljs.core.instance?},
    pr-str-with-opts {:name cljs.core.pr-str-with-opts},
    sequential? {:name cljs.core.sequential?},
    fn? {:name cljs.core.fn?},
    empty {:name cljs.core.empty},
    dorun {:name cljs.core.dorun},
    remove-method {:name cljs.core.remove-method},
    gensym {:name cljs.core.gensym},
    not= {:name cljs.core.not=},
    bit-or {:name cljs.core.bit-or},
    add-watch {:name cljs.core.add-watch},
    some {:name cljs.core.some},
    nil? {:name cljs.core.nil?},
    string? {:name cljs.core.string?},
    second {:name cljs.core.second},
    keys {:name cljs.core.keys},
    bit-set {:name cljs.core.bit-set},
    false? {:name cljs.core.false?},
    true? {:name cljs.core.true?},
    repeat {:name cljs.core.repeat},
    zipmap {:name cljs.core.zipmap},
    distinct {:name cljs.core.distinct},
    string-print {:name cljs.core.string-print},
    get-in {:name cljs.core.get-in},
    bit-xor {:name cljs.core.bit-xor},
    complement {:name cljs.core.complement},
    get-validator {:name cljs.core.get-validator},
    js->clj {:name cljs.core.js->clj},
    derive {:name cljs.core.derive},
    partition-by {:name cljs.core.partition-by},
    rem {:name cljs.core.rem},
    odd? {:name cljs.core.odd?},
    symbol? {:name cljs.core.symbol?},
    js-obj {:name cljs.core.js-obj},
    re-matches {:name cljs.core.re-matches},
    split-with {:name cljs.core.split-with},
    spread {:name cljs.core.spread},
    next {:name cljs.core.next},
    symbol {:name cljs.core.symbol},
    vals {:name cljs.core.vals},
    select-keys {:name cljs.core.select-keys},
    rand {:name cljs.core.rand},
    deref {:name cljs.core.deref},
    make-hierarchy {:name cljs.core.make-hierarchy},
    + {:name cljs.core.+},
    number? {:name cljs.core.number?},
    descendants {:name cljs.core.descendants},
    last {:name cljs.core.last},
    some-fn {:name cljs.core.some-fn},
    integer? {:name cljs.core.integer?},
    prn {:name cljs.core.prn},
    with-meta {:name cljs.core.with-meta},
    * {:name cljs.core.*},
    butlast {:name cljs.core.butlast},
    - {:name cljs.core.-},
    seq? {:name cljs.core.seq?},
    identical? {:name cljs.core.identical?},
    pr-sequential {:name cljs.core.pr-sequential},
    vary-meta {:name cljs.core.vary-meta},
    bit-flip {:name cljs.core.bit-flip},
    zero? {:name cljs.core.zero?},
    bit-and {:name cljs.core.bit-and},
    newline {:name cljs.core.newline},
    replicate {:name cljs.core.replicate},
    keep-indexed {:name cljs.core.keep-indexed},
    distinct? {:name cljs.core.distinct?},
    vec {:name cljs.core.vec},
    concat {:name cljs.core.concat},
    update-in {:name cljs.core.update-in},
    vector {:name cljs.core.vector},
    conj {:name cljs.core.conj},
    / {:name cljs.core._SLASH_},
    assoc {:name cljs.core.assoc},
    boolean {:name cljs.core.boolean},
    neg? {:name cljs.core.neg?},
    js-delete {:name cljs.core.js-delete},
    isa? {:name cljs.core.isa?},
    remove-watch {:name cljs.core.remove-watch},
    vector? {:name cljs.core.vector?},
    split-at {:name cljs.core.split-at},
    map {:name cljs.core.map},
    counted? {:name cljs.core.counted?},
    frequencies {:name cljs.core.frequencies},
    rand-int {:name cljs.core.rand-int},
    iterate {:name cljs.core.iterate},
    mapcat {:name cljs.core.mapcat},
    assoc-in {:name cljs.core.assoc-in},
    inc {:name cljs.core.inc},
    every-pred {:name cljs.core.every-pred},
    re-find {:name cljs.core.re-find},
    bit-not {:name cljs.core.bit-not},
    seq {:name cljs.core.seq},
    filter {:name cljs.core.filter},
    js-keys {:name cljs.core.js-keys},
    alter-meta! {:name cljs.core.alter-meta!},
    re-seq {:name cljs.core.re-seq},
    empty? {:name cljs.core.empty?},
    name {:name cljs.core.name},
    aset {:name cljs.core.aset},
    nnext {:name cljs.core.nnext},
    doall {:name cljs.core.doall},
    not-any? {:name cljs.core.not-any?},
    reductions {:name cljs.core.reductions},
    into {:name cljs.core.into},
    ffirst {:name cljs.core.ffirst},
    bit-clear {:name cljs.core.bit-clear},
    hash {:name cljs.core.hash},
    associative? {:name cljs.core.associative?},
    drop-last {:name cljs.core.drop-last},
    replace {:name cljs.core.replace},
    parents {:name cljs.core.parents},
    map? {:name cljs.core.map?},
    prefers {:name cljs.core.prefers},
    quot {:name cljs.core.quot},
    reverse {:name cljs.core.reverse},
    count {:name cljs.core.count},
    set {:name cljs.core.set},
    fn->comparator {:name cljs.core.fn->comparator},
    comp {:name cljs.core.comp},
    nth {:name cljs.core.nth},
    constantly {:name cljs.core.constantly},
    namespace {:name cljs.core.namespace},
    pr-str {:name cljs.core.pr-str},
    < {:name cljs.core.<},
    sort-by {:name cljs.core.sort-by},
    cycle {:name cljs.core.cycle},
    peek {:name cljs.core.peek},
    pr-with-opts {:name cljs.core.pr-with-opts},
    reduce {:name cljs.core.reduce},
    interleave {:name cljs.core.interleave},
    cons {:name cljs.core.cons},
    str {:name cljs.core.str},
    remove-all-methods {:name cljs.core.remove-all-methods},
    first {:name cljs.core.first},
    = {:name cljs.core.=},
    memoize {:name cljs.core.memoize},
    range {:name cljs.core.range},
    tree-seq {:name cljs.core.tree-seq},
    set-validator! {:name cljs.core.set-validator!},
    prefer-method {:name cljs.core.prefer-method},
    partition-all {:name cljs.core.partition-all},
    not-every? {:name cljs.core.not-every?},
    > {:name cljs.core.>},
    max {:name cljs.core.max},
    identity {:name cljs.core.identity},
    fnext {:name cljs.core.fnext},
    min-key {:name cljs.core.min-key},
    reset-meta! {:name cljs.core.reset-meta!},
    array {:name cljs.core.array},
    subs {:name cljs.core.subs},
    >= {:name cljs.core.>=},
    reset! {:name cljs.core.reset!},
    even? {:name cljs.core.even?},
    bit-shift-left {:name cljs.core.bit-shift-left},
    methods {:name cljs.core.methods},
    compare {:name cljs.core.compare},
    group-by {:name cljs.core.group-by},
    get {:name cljs.core.get},
    <= {:name cljs.core.<=},
    fnil {:name cljs.core.fnil},
    force {:name cljs.core.force},
    partial {:name cljs.core.partial},
    array-seq {:name cljs.core.array-seq},
    pos? {:name cljs.core.pos?},
    take-while {:name cljs.core.take-while},
    underive {:name cljs.core.underive},
    ancestors {:name cljs.core.ancestors},
    hash-combine {:name cljs.core.hash-combine},
    partition {:name cljs.core.partition},
    map-indexed {:name cljs.core.map-indexed},
    contains? {:name cljs.core.contains?},
    interpose {:name cljs.core.interpose},
    delay {:name cljs.core.delay},
    apply {:name cljs.core.apply},
    swap! {:name cljs.core.swap!},
    sum {:name plugh.core.sum},
    avg {:name plugh.core.avg},
    subvec {:name cljs.core.subvec},
    rest {:name cljs.core.rest},
    keyword {:name cljs.core.keyword},
    to-num {:name plugh.core.to-num}
    mod {:name cljs.core.mod},
    nfirst {:name cljs.core.nfirst},
    nthnext {:name cljs.core.nthnext},
    dec {:name cljs.core.dec},
    undefined? {:name cljs.core.undefined?},
    println {:name cljs.core.println},
    aget {:name cljs.core.aget},
    pr {:name cljs.core.pr},
    drop {:name cljs.core.drop},
    aclone {:name cljs.core.aclone},
    pop {:name cljs.core.pop},
    atom {:name cljs.core.atom},
    bit-shift-right {:name cljs.core.bit-shift-right},
    delay? {:name cljs.core.delay?},
    realized? {:name cljs.core.realized?},
    disj {:name cljs.core.disj},
    merge-with {:name cljs.core.merge-with},
    take-nth {:name cljs.core.take-nth},
    take-last {:name cljs.core.take-last},
    take {:name cljs.core.take},
    set? {:name cljs.core.set?},
    rand-nth {:name cljs.core.rand-nth},
    juxt {:name cljs.core.juxt},
    alength {:name cljs.core.alength},
    to-array {:name cljs.core.to-array},
    hash-map {:name cljs.core.hash-map},
    bit-and-not {:name cljs.core.bit-and-not},
    compare-and-set! {:name cljs.core.compare-and-set!},
    type {:name cljs.core.type},
    repeatedly {:name cljs.core.repeatedly},
    trampoline {:name cljs.core.trampoline},
    remove {:name cljs.core.remove},
    find {:name cljs.core.find},
    coll? {:name cljs.core.coll?},
    drop-while {:name cljs.core.drop-while},
    not-empty {:name cljs.core.not-empty},
    flatten {:name cljs.core.flatten},
    list {:name cljs.core.list},
    every? {:name cljs.core.every?},
    flush {:name cljs.core.flush},
    sort {:name cljs.core.sort},
    dissoc {:name cljs.core.dissoc},
    not {:name cljs.core.not},
    get-method {:name cljs.core.get-method},
    merge {:name cljs.core.merge},
    min {:name cljs.core.min},
    bit-test {:name cljs.core.bit-test},
    keep {:name cljs.core.keep},
    meta {:name cljs.core.meta},
    prim-seq {:name cljs.core.prim-seq}})

(defn load-core-names []
  core-names)

(def cljs-macros (quote #{== time doseq bit-or nil? for bit-set false? true? bit-xor dotimes defmethod + this-as defrecord * - identical? bit-flip zero? bit-and / neg? assert inc bit-not aset bit-clear extend-type condp < amap > max >= bit-shift-left deftype <= pos? defmulti reify mod dec undefined? aget try bit-shift-right lazy-seq areduce alength defprotocol bit-and-not satisfies? assert-args binding min bit-test}))

(def clojure-macros (set '[-> ->> .. some-> and assert comment cond declare defn defn-
                           doto extend-protocol fn for if-let if-not let letfn loop
                           or when when-first when-let when-not while]))

(declare exp)

(defn build [action locals expr opt pp]
  {:result
   (binding [ana/*cljs-ns* 'cljs.user]
     (let [env {:ns (@ana/namespaces ana/*cljs-ns*)
                :uses #{'cljs.core}
                :context :expr
                :locals locals}]
       (with-redefs [ana/get-expander exp]
         (action env expr))))
   :status 200})

(def compilation (partial build
                          #(comp/emit-str (ana/analyze % %2))
                          (load-core-names)))

(def analyze (partial build
                      #(ana/analyze % %2)
                      {}))

(defn read-forms [s]
  (let [pr (new java.io.PushbackReader (new java.io.StringReader s))]
    (letfn [(read-next [] 
                       (binding [r/*read-eval* false]
                         (let [next (r/read pr false ::end-of-thing)]
                               (if (= ::end-of-thing next) nil (cons next (lazy-seq (read-next))))
                               )))]
      (read-next))
    ))

(defn append-not-nil [coll & rest] 
  (let [c2 (into [] coll)
        r2 (filter #(not (nil? %)) rest)]
    (reduce conj c2 r2)))

(defn replace-in [what the-map]
  (cond
    (and (symbol? what) (. (str what) endsWith "#"))
    (get the-map what)
    (vector? what) (into [] (map #(replace-in % the-map) what))
    (seq? what) (map #(replace-in % the-map) what)
    :else what
    ))

(defn build-a-func [line]
  (let [[kw & func] line
        master '(fn [it-line] (if (contains? it-line kw#) 
                                (assoc {} kw# ((fn [it] func#) (kw# it-line)))
                                (assoc {} kw# ((fn [it] func#) it-line))
                                ))
        the-map {'kw# kw 'func# (mapcat (fn [q] (if (seq? q) q [q])) func)}
        ]
    (replace-in master the-map)
    ))

(defn build-from-named [named]
  (if (or (not named) (empty? named))
    nil
    (let [funcs (into [] (map build-a-func named))
          master '(fn [array] 
                   (map (fn [it-line] 
                          (reduce conj {} 
                                  (map (fn [the-func] 
                                         (the-func it-line)) all-funcs#)))
                                   array))
          the-map {'all-funcs# funcs}] 
      (replace-in master the-map)
      )
    ))

(defn- hash-keyword? [x]
  (and (keyword? x) (. (name x) startsWith "#")))

(defn- not-hash-keyword? [x]
  (and (keyword? x) (not (. (name x) startsWith "#"))))

(defn- wrap-total [func t-fn]
  (if t-fn 
    (let [master '(fn [lines]
                   (let [ret-a (into [] (func# lines))
                         total (into {} (map (fn [[k f]] [k (f (map k ret-a))]) pairs#))]
                     (conj ret-a total)
                     )
                   )]
      (replace-in master {'func# func 
                          'pairs# (into [] (map #(into [] %) (partition 2 (rest t-fn))))}))
    func))

(defn- wrap-last [lst f-func t-func]
  (let [most (butlast lst)
        the-last (wrap-total (last lst) (first t-func))
        ]
    (conj (into [] most)
          (reduce (fn [inner c-fil] 
                    (let [master '(fn [lines] (inner# (filter (fn [it] func#) lines)))]
                      (replace-in master {'inner# inner 'func# (rest c-fil)}))
                    ) the-last f-func)
          )))

(defn compile-string [s] 
  (let [org-forms (read-forms s)
        filter-func (filter #(-> % first (= :#filter)) org-forms)
        total-func (filter #(-> % first (= :#total)) org-forms)
        named (filter #(-> % first not-hash-keyword?) org-forms)
        funcs (filter #(-> % first keyword? not) org-forms)
        rewritten (wrap-last 
                    (append-not-nil funcs (build-from-named named))
                    filter-func total-func)]
    (let [ret (map #(:result (compilation % :simple false)) rewritten)]
      ret
      )))

;; privates

(defn- exp [sym env]
  (let [mvar
        (when-not (or (-> env :locals sym) ;locals hide macros
                      (-> env :ns :excludes sym))
          (if-let [nstr (namespace sym)]
            (when-let [ns (cond
                           (= "clojure.core" nstr) (find-ns 'cljs.core)
                           (.contains nstr ".") (find-ns (symbol nstr))
                           :else
                           (-> env :ns :requires-macros (get (symbol nstr))))]
              (.findInternedVar ^clojure.lang.Namespace ns (symbol (name sym))))
            (if-let [nsym (-> env :ns :uses-macros sym)]
              (.findInternedVar ^clojure.lang.Namespace (find-ns nsym) sym)
              (.findInternedVar ^clojure.lang.Namespace (find-ns 'cljs.core) sym))))]
    (let [sym (symbol (.getName sym))]
      (when (and mvar (or (clojure-macros sym) (cljs-macros sym)))
        @mvar))))


