(ns plugh.sloth
  (:use-macros [cljs.core.logic.macros :only [run* conde == defrel fact]])
  (:require [cljs.core.logic :as cl]))


(.log js/console "I am sloth")


(def woff #(.log js/console %))