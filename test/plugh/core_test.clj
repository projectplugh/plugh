(ns plugh.core-test
  (:use clojure.test
        midje.sweet
        plugh.core
        plugh.visi.parser))

(deftest a-test
  (testing "FIXME, I fail."
    (is (= 1 1))))

(fact "I like elephants"
      1 => 1)

(fact "I can parse"
      (visi-parser "\"hi\" = 55")
      )