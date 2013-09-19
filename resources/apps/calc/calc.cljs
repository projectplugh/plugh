(ns demo.calc
  (:require-macros
    [cljs.core.async.macros :as m :refer [go]]
    [clang.angular :refer [def.controller defn.scope def.filter in-scope s-set fnj]])
  (:use [clang.util :only [? module]])
  (:require [plugh.core :as pc]
            [cljs.core.async :as async
             :refer [<! >! chan timeout]]))


(def compiler-chan (pc/server-chan "cljs compiler"))

(def.controller pc/m Visicalc [$scope $compile]
  (let [update-func (fn [name org-data]
    (let [kw (keyword name)
          okw (keyword (str name "_opt"))
          ocd (keyword (str name "_cdefs"))
          data (or org-data (:myData $scope))
          func (-> $scope okw :mapFunc)
          revised (-> data js->clj func)
          unique-keys (into [] (into #{} (flatten (map keys revised))))
          col-names (clj->js (map (fn [q] {:field q}) unique-keys))]
      (s-set ocd col-names)
      (s-set kw (clj->js revised))
      ))]
  
  (assoc! $scope :myData (clj->js [{:name "David" :age 49}
                                   {:name "Archer" :age 10}
                                   {:name "Tessa" :age 1}
                                   {:name "Sloth" :age 2}]))
  
  (s-set :gridOptions 
         (clj->js 
           {:data "myData"
            :enableCellSelection true,
            :multiSelect false,
            :enableCellEdit true,
            :columnDefs [{ :field "name", :displayName "First Name", 
                          :width "*"  :enableCellEdit true}
                         { :field "age", :cellClass "ageCell", 
                          :headerClass "ageHeader", :width "**"  
                          :enableCellEdit true}]
            }))
  
  
  (defn.scope updateFunc [id]
    (let [on (keyword (str id "_opt"))]
      (let [code (-> $scope on :mapText)
            ch (chan)]
        (go (>! compiler-chan {:from ch :source code}))
        (letfn 
          [
           (update-funcs
             [func-text]
             (if func-text
               (do
                 (let [ll (last func-text)
                       bl (butlast func-text)
                       text (str "function func_" id "(var_" id ") {\n"
                                 "if (!cljs) cljs = {};\n"
                                 "if (!cljs.user) cljs.user = {};\n"
                                 (clojure.string/join ";\n" bl)
                                 ";\n"
                                 "return (" 
                                 ll 
                                 ")( var_" 
                                 id ");}"
                                 "func_"
                                 id)
                       the-func (js/eval text)]
                   (assoc! (on $scope) :mapFunc the-func)
                   
                   (update-func id nil)
                   (.$digest $scope)))))
           (show-err [err] (if err (js/alert (str "Compile error: " err))))]
          (go (let [res (<! ch)
                    func-text (:result res)
                    err (:error res)]
                (update-funcs func-text)
                (show-err err))
              (async/close! ch)
              )))))

  
  (defn.scope more []
    (let [name (pc/mguid)
          opt-name (str name "_opt")
          okw (keyword opt-name)]
      (s-set okw (clj->js {:data name 
                                          :columnDefs (str name "_cdefs")
                                          :mapFunc (fn [x] x)
                                          :lastSeen 0
                                          :mapText ""}))
      (update-func name nil)
      (.$watch $scope "myData" (fn [new-value old-value] 
                                 (do 
                                   (let [now (.now js/Date)]
                                     (assoc! (okw $scope) :lastSeen now)
                                     (go (let [to (<! (timeout 500))
                                               last-seen (-> $scope okw :lastSeen)]
                                           (if (= now last-seen) (in-scope (update-func name new-value)))
                                           ))))) true)
      ;; (.$watch $scope name (fn [new-value old-value] (.log js/console "Got watch event for " name " at " (pc/mguid))))
      
      (let [
            compiled (($compile (str "<hr>
                                     <div class='gridStyle' ng-grid='" opt-name "' style='height: 150px'></div>
                                     <textarea cols=\"100\" ng-model='" opt-name ".mapText'></textarea><button ng-click=\"updateFunc('" name "')\">Calc</button>")) $scope)]
        (.append (js/$ "#thingy")  compiled))
      
      (assoc! $scope :myData (clj->js (conj (js->clj (:myData $scope)) {:name "wombat" :age 44})))))
  
  ;; (defn as-int [x] (if (string? x) (js/parseInt x) x))

  ;; (fn [x] (map (fn [q] (assoc q :age (+ 2 (as-int (:age q)))) x))

  ;; (fn [x] (map (fn [q] {:name (:name q) :age (+ 1 (:age q))}) x))
  
))