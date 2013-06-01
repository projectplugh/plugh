(ns plugh.misc-test
  (:use clojure.test
        plugh.util.misc
        [clojure.core.match :only (match)]))

(defn truth [& x] true)

(deftest match-test
  (testing "simple match"
    (is (=
          (match [33] [x :guard odd?] (+ 11 x)
            [x] (+ 55 x)) 44))))

(deftest func-test
  (testing "simple function match"
    (let [fn (match-func
               [x :guard odd?] (+ 11 x)
               [x] (+ 55 x))]
      (is (= (fn 33) 44)))))

(deftest pfunc-test
  (testing "simple function match"
    (let [fn (match-pfunc
               [x :guard odd?] (+ 11 x)
               [x] (+ 55 x))]
      (is (= (fn 33) 44))
      (is (= (fn :defined? 33) true)))))

(def foo-guard
  (match-pfunc
    [x :guard number?] (+ 11 x)
    [{:foo (x :guard truth)}] x
    [{:bar (x :guard number?)}] (* 5 x)
    [{:sloth (x :guard number?)}] (* 99 x)
    [{:bar (x :guard truth)}] x))

(def baz-guard
  (match-pfunc
    [{:baz (x :guard truth)}] x
    [{:moo (x :guard number?)}] (* 5 x)
    [{:moo (x :guard truth)}] x))

(deftest match-test2
  (testing "simple match"
    (is (=
          (match
            [{:foo 44, :bar "hi"}]
            [x :guard number?] (+ 11 x)
            [({:foo (x :guard truth)} :guard truth)] x
            [({:bar x } :guard truth)] x) 44))
    (is (=
          (match [{:foo3 44, :bar "hi"}] [x :guard number?] (+ 11 x)
            [({:foo (x :guard truth)} :guard truth)] x
            [({:bar x} :guard truth)] x) "hi"))
    ))

(deftest func-test
  (testing "simple function match"
    (let [fn (match-func
               [x :guard number?] (+ 11 x)
               [{:foo (x :guard truth)}] x
               [{:bar (x :guard truth)}] x)]
      (is (= (fn 33) 44))
      (is (= (fn "hi") nil))
      (is (= (fn {:foo 88}) 88))
      (is (= (fn {:foo3 88, :bar "hi"}) "hi"))
      (is (= (fn {:baz 88}) nil))
      )))

(deftest pfunc-test
  (testing "simple function match"
    (let [fn foo-guard]
      (is (= (fn 33) 44))
      (is (= (fn "hi") nil))
      (is (= (fn {:foo 88}) 88))
      (is (= (fn {:foo3 88, :bar "hi"}) "hi"))
      (is (= (fn {:baz 88}) nil))
      (is (not (fn :defined? {:baz 88})))
      (is (fn :defined? {:bar 88}))
      (is (fn :defined? {:sloth 77}))
      (is (not (fn :defined? {:sloth "hi"})))
      (is (not (fn :defined? {:moo "hi"})))
      )))

(deftest or-else-test
  (testing "function concat"
    (let [fn (or-else foo-guard baz-guard)]
      (is (= (fn 33) 44))
      (is (= (fn "hi") nil))
      (is (= (fn {:foo 88}) 88))
      (is (= (fn {:foo3 88, :bar "hi"}) "hi"))
      (is (= (fn {:baz33 88}) nil))
      (is (fn :defined? {:baz 88}))
      (is (fn :defined? {:bar 88}))
      (is (fn :defined? {:sloth 77}))
      (is (not (fn :defined? {:sloth "hi"})))
      (is (fn :defined? {:moo "hi"}))
    )))


(deftest future-test
  (testing "Futures"
    (let [t (. Thread currentThread)
          a (promise)
          b (promise)
          fut (pfuture (. Thread sleep 300) 44)]
      (on-done fut (fn [x]
                     (deliver a x)
                     (is (not (= t (. Thread currentThread))))
                     ))
      (is (= @fut @a))
      (on-done fut (fn [x]
                     (deliver b x)
                     (is (= t (. Thread currentThread)))
                     ))
      (is (= @fut @b))
      )))

(deftest future-test-exception
  (testing "Futures & exeptions"
    (let [t (. Thread currentThread)
          a (promise)
          b (promise)
          fut (pfuture (. Thread sleep 300) (/ 0 0))]
      (on-fail fut (fn [x]
                     (deliver a x)
                     (is (not (= t (. Thread currentThread))))
                     ))
      (is (instance? Exception @a))
      (on-fail fut (fn [x]
                     (deliver b x)
                     (is (= t (. Thread currentThread)))
                     ))
      (is (instance? Exception @b))
      )))

(deftest deref-test
  (testing "Does deref? work?"
    (is (deref? (promise)))
    (is (not (deref? "foo")))
    ))
