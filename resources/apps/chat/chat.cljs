(ns demo.chat
  (:require-macros
    [cljs.core.async.macros :as m :refer [go]]
    [clang.angular :refer [def.controller defn.scope def.filter in-scope s-set fnj]])
  (:use [clang.util :only [? module]])
  (:require [plugh.core :as pc]
            [cljs.core.async :as async
             :refer [<! >! chan]]))

(def server-chan (pc/server-chan "The Chat Server"))

(def compiler-chan (pc/server-chan "cljs compiler"))

(def.controller pc/m Chatter [$scope $compile]
  (let [update-func (fn [name org-data]
    (let [kw (keyword name)
          okw (keyword (str name "_opt"))
          data (or org-data (:myData $scope))
          func (-> $scope okw :mapFunc)
          revised (-> data js->clj func clj->js)]
      (s-set kw revised)
      ))]
  
  (assoc! $scope :chats (clj->js []))
  
  (assoc! $scope :myData (clj->js [{:name "David" :age 49}
                                   {:name "Archer" :age 10}
                                   {:name "Tessa" :age 1}
                                   {:name "Sloth" :age 2}]))
  (s-set :myCols (clj->js [
        {:label "First Name", :map "name" :isEditable true  :type "text"},
        {:label "Age", :map "age", :isEditable true, :type "number"},
    ]))
  
  (s-set :globalConfig (clj->js {
        :isPaginationEnabled false
    }))
  
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
  
  
  (assoc! $scope :line "")
    
  (defn.scope send [] 
    (let [msg (:line $scope)]
      (go 
        (>! server-chan {:msg msg})))
    (assoc! $scope :line ""))
  
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
          opt-name (str name "_opt")]
      (assoc! $scope (keyword opt-name) (clj->js {:data name 
                                                  :mapFunc (fn [x] x)
                                                  :mapText ""}))
      (update-func name nil)
      (.$watch $scope "myData" (fn [new-value old-value] 
                                 (do 
                                   (update-func name new-value))) true)
      ;; (.$watch $scope name (fn [new-value old-value] (.log js/console "Got watch event for " name " at " (pc/mguid))))
      
      (let [
            compiled (($compile (str "<hr>
                                     <div class='gridStyle' ng-grid='" opt-name "' style='height: 200px'></div>
                                     <textarea width="300" ng-model='" opt-name ".mapText'></textarea><button ng-click=\"updateFunc('" name "')\">Calc</button>")) $scope)]
        (.append (js/$ "#thingy")  compiled))
      
      (assoc! $scope :myData (clj->js (conj (js->clj (:myData $scope)) {:name "wombat" :age 44})))))
  
  ;; (defn as-int [x] (if (string? x) (js/parseInt x) x))

  ;; (fn [x] (map (fn [q] (assoc q :age (+ 2 (as-int (:age q)))) x))

  ;; (fn [x] (map (fn [q] {:name (:name q) :age (+ 1 (:age q))}) x))
  
  (let [rc (chan)]
    (go (>! server-chan {:add rc}))
    (letfn [(proc [] 
                  (go (let [chats (<! rc)]
                        (in-scope (doseq [m chats] (.push (:chats $scope) m)))
                        (proc))))]
      (proc))
    )))
    
    