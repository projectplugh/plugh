(ns plugh.util.misc
  (:gen-class)
  (:use [clojure.core.match :only (match)])
  )

(defmacro match-func [& body]
  "Create a function that does pattern matching."
  `(fn [x#] (match [x#] ~@body)))

(defmacro match-pfunc [& body]
  "Create a partial function that does pattern matching."
  (let [rewrite (mapcat (fn [x] [(first x) true]) (partition 2 body))]
    `(fn ([x#] (match [x#] ~@body))
       ([x# y#]
         (cond
           (= :defined? x#)
           (match [y#] ~@rewrite)
           (= :body x#)
           '(~@body))))))

(defn or-else
  [it & rest]
  "Chain a bunch of Partial Functions together"
  (let [all (cons it rest)]
    (fn ([x] (let [func (first (filter (fn [ff]
                                         (ff :defined? x)) all))]
              (if func
                (func x)
                (it x))))
      ([x y] (let [func (first (filter (fn [ff] (ff :defined? y)) all))]
             (cond
               (= :defined? x) (if func true false)
               (= :body x) (reduce concat (map (fn [thing] (thing :body nil)) all)))
             )))))

(defn all-done [fut val]
  (let [funcs (map (fn [func] (future (func val))) @(:done (meta fut)))]
    (reset! (:complete (meta fut)) true)
    (dorun funcs) ;; force evaluation
    (reset! (:done (meta fut)) [])
    ))

(defn fail-done [fut val]
  (let [funcs (map (fn [func] (future (func val))) @(:fail (meta fut)))]
    (reset! (:fail-exception (meta fut)) val)
    (reset! (:complete (meta fut)) true)
    (dorun funcs) ;; force evaluation
    (reset! (:fail (meta fut)) [])
    ))

(defmacro bfuture [& body]
  `(let [prom# (promise)
          fut#
         (with-meta
           (future
             (try
               (let [ret# (do ~@body)]
                 (all-done @prom# ret#)
                 ret#)
               (catch Exception e# (fail-done @prom# e#))))
           {:complete (atom false)
            :done (atom [])
            :fail-exception (atom nil)
            :fail (atom [])})]
     (deliver prom# fut#)
     fut#))

(defn on-done [fut func]
  (if @(:complete (meta fut))
    (func @fut)
    (swap! (:done (meta fut)) (fn [x] (cons func x)))))

(defn on-fail [fut func]
  (if @(:complete (meta fut))
    (func @(:fail-exception (meta fut)))
    (swap! (:fail (meta fut)) (fn [x] (cons func x)))))