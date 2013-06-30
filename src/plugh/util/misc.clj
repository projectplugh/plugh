(ns plugh.util.misc
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
          (match [y#] ~@rewrite :else false)
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
  (locking fut
    (let [funcs (map (fn [func] (future (func val))) @(:done (meta fut)))]
      (reset! (:complete (meta fut)) true)
      (dorun funcs) ;; force evaluation
      (reset! (:done (meta fut)) [])
      )))

(defn fail-done [fut val]
  (locking fut
    (let [funcs (map (fn [func] (future (func val))) @(:fail (meta fut)))]
      (reset! (:fail-exception (meta fut)) val)
      (reset! (:complete (meta fut)) true)
      (dorun funcs) ;; force evaluation
      (reset! (:fail (meta fut)) [])
      )))

(defmacro pfuture [& body]
  "Build a Future that allows for registration of on-done and on-fail
  events so that one does not have to sit around on a thread waiting
  for the future"
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
  "Call with a Future created via pfuture and a function.
  When the pfuture is done and the result was not throwing
  an exception, invoke the function. If the pfuture was
  complete before this call, invoke the function immediately"
  (let [todo (locking fut
               (if @(:complete (meta fut))
                 (fn [] (func @fut))
                 (do
                   (swap! (:done (meta fut)) (fn [x] (cons func x)))
                   (fn [] nil)
                   )
                 ))]
    (todo)))

(defn on-fail [fut func]
  "Call with a Future created via pfuture and a function.
  When the pfuture is done and the result was throwing
  an exception, invoke the function with the exception. If the pfuture was
  complete before this call, invoke the function immediately"
  (let [todo (locking fut
               (if @(:complete (meta fut))
                 (fn [] (func @(:fail-exception (meta fut))))
                 (do
                   (swap! (:fail (meta fut)) (fn [x] (cons func x)))
                   (fn [] nil)
                   )))]
    (todo)))


(defn deref? [it]
  "Is it something that deref can be called on?"
  (instance? clojure.lang.IDeref it))

(defmacro print-time [& body]
  "Executes the code and prints the milliseconds for run time"
  `(let [start# (. java.lang.System currentTimeMillis)]
     (try (do ~@body)
       (finally (println "Execution time " (- (. java.lang.System currentTimeMillis) start#)))
     )))

(defn id [x]
  "The identity function"
   x)
