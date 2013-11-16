(ns frog.dog 
  (:require-macros
    [cljs.core.async.macros :as async])
  (:require [cljs.core.async :as async :refer [mult tap map< go chan timeout]]
            [plugh.util.sent-analysis :as analisys :refer [calc-sentiment xform xfilter flow]]))
(declare twitter with-sentiment filtered-sentiment average-sentiment with-sentiment filtered-sentiment average-sentiment)
(def twitter (chan))
(def twitter$mult (delay (mult twitter)))
(defn add-sentiment [tweet] (plugh.util.misc/--> 
                              (plugh.util.misc/--> tweet :text) 
                              calc-sentiment (fn [G1168830776822148Z37478996210] 
                                               (plugh.util.misc/arrow-assignment (plugh.util.misc/--> tweet :sentiment)
                                                                                  G1168830776822148Z37478996210))))
(def with-sentiment (delay (let [twitter$ (tap (deref twitter$mult) (chan))] (xform add-sentiment twitter$))))
(def with-sentiment$mult (delay (mult (deref with-sentiment))))
(def filtered-sentiment (delay (let [with-sentiment$ (tap (deref with-sentiment$mult) (chan))] 
                                 (xfilter 
                                   (fn [x] (let [sent (plugh.util.misc/--> x :sentiment)] 
                                             (or (> (plugh.util.misc/--> sent :pos) 1) 
                                                 (< (plugh.util.misc/--> sent :neg) -1)))) with-sentiment$))))
(def filtered-sentiment$mult (delay (mult (deref filtered-sentiment))))
(def average-sentiment (delay (let [filtered-sentiment$ (tap (deref filtered-sentiment$mult) (chan))]
                                 (flow (fn [c t] 
                                         (plugh.util.misc/arrow-update-func 
                                           c
                                           {:pos-sum (fn [G1168830776822146Z66658096905] 
                                                       (+ G1168830776822146Z66658096905 
                                                          (plugh.util.misc/--> t :sentiment :pos))),
                                             :neg-sum (fn [G1168830776822147Z31739067034] 
                                                        (+ G1168830776822147Z31739067034 
                                                           (plugh.util.misc/--> t :sentiment :neg))),
                                              :pos-cnt inc, :neg-cnt inc}))
                                        {:pos-sum 0, :neg-sum 0, :pos-cnt 0, :neg-cnt 0} filtered-sentiment$))))
(def average-sentiment$mult (delay (mult (deref average-sentiment))))
(def averages (delay (mult (let [average-sentiment$ (tap (deref average-sentiment$mult) (chan))] average-sentiment$))))
