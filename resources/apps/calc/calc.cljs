(ns demo.calc
  (:require-macros
    [cljs.core.async.macros :as m :refer [go]]
    [clang.angular :refer [def.controller defn.scope def.filter in-scope s-set fnj]])
  (:use [clang.util :only [? module]])
  (:require [plugh.core :as pc]
            [cljs.core.async :as async
             :refer [<! >! chan timeout]]))


(def compiler-chan (pc/server-chan "cljs compiler"))

(def visi-chan (pc/server-chan "visi-dispatch"))

(defn send-to-chan [chan info]
  (str 
    (map 
      (fn [x] 
        (do 
          (.log js/console "sending " (str x))
          (go (>! chan x)))) info)))

(defn read-from-chan [chan func]
  (go 
    (let [v (<! chan)]
      (.log js/console "Read " (str v))
      (func v)
      (read-from-chan chan func)
      )))


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
  
  (s-set :sentiment (clj->js  {:neg-cnt nil, :neg-sum "", :pos-sum "", :pos-cnt ""}))
  
  (s-set :visiCode "")
  
  (s-set :sample [{:text
    "RT @USATODAY: Here's where the current #shutdown stands: http://t.co/YI8NQ7s5wB",
    :id 386496793292570624,
    :created_at "Sat Oct 05 14:23:10 +0000 2013"}
   {:text
    "Great article by @ezraklein Republican Cilvil War\nhttp://t.co/UoWqCyHk1z #shutdown",
    :id 386496819141672960,
    :created_at "Sat Oct 05 14:23:17 +0000 2013"}
   {:text
    "RT @Forbes: What caused the U.S. government #shutdown? http://t.co/GTPTG7TJfu",
    :id 386496819661774848,
    :created_at "Sat Oct 05 14:23:17 +0000 2013"}
   {:text
    "RT @USATODAY: Here's where the current #shutdown stands: http://t.co/YI8NQ7s5wB",
    :id 386496852188622852,
    :created_at "Sat Oct 05 14:23:24 +0000 2013"}
   {:text ".@Barackobama End the #shutdown! #DefundObamacare",
    :id 386496855804100608,
    :created_at "Sat Oct 05 14:23:25 +0000 2013"}
   {:text "What about this #shutdown",
    :id 386496857716695040,
    :created_at "Sat Oct 05 14:23:26 +0000 2013"}
   {:text
    "The #HouseGOP the extremists in the #TeaParty.  RT @Forbes: What caused the U.S. government #shutdown? http://t.co/AxOmCJVBEZ",
    :id 386496867170664448,
    :created_at "Sat Oct 05 14:23:28 +0000 2013"}
   {:text
    "RT @USATODAY: Here's where the current #shutdown stands: http://t.co/YI8NQ7s5wB",
    :id 386496876045819904,
    :created_at "Sat Oct 05 14:23:30 +0000 2013"}
   {:text
    "RT @SpeakerBoehner: WH says govt #shutdown “doesn’t really matter” http://t.co/MyjWJmVUIO GOP continues taking action to keep critical part…",
    :id 386496877178662912,
    :created_at "Sat Oct 05 14:23:30 +0000 2013"}
   {:text
    "RT @traciglee: \"We cannot have a wholesale #shutdown and a piecemeal startup.\" http://t.co/JhjDbtYtaz",
    :id 386496881078960129,
    :created_at "Sat Oct 05 14:23:31 +0000 2013"}
   {:text
    "RT @zypldot: Don't let the #shutdown die down this weekend! #BlameHarryReid and let @SpeakerBoehner now we #StandWithBoehner. @SenTedCruz",
    :id 386496881867517952,
    :created_at "Sat Oct 05 14:23:32 +0000 2013"}
   {:text
    "RT @TangiQuemener: Pendant ce temps, tempête #Karen remonte vers les côtes du golfe du Mexique. Personnel d'urgence fédéral mobilisé, mais …",
    :id 386496888352284672,
    :created_at "Sat Oct 05 14:23:33 +0000 2013"}])
    
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
  
  (defn.scope runTheVisiDemo []
    (let [the-code (:visiCode $scope)
          ch (chan)]
      (.log js/console "the code is " the-code)
      (go (>! visi-chan {:cmd "run-demo-code" :to-chan ch :code the-code}))
      (go
        (loop []
          (let [v (<! ch)]
            (if (not v) [] (do 
                             ;; (.log js/console "Got from server " v)
                             (in-scope (s-set :sentiment (clj->js v)))
                             (recur))
              ))))))
  
  (defn.scope compileVisiToJs []
    (let [the-code (:visiCode $scope)
          ch (chan)]
      (go (>! visi-chan {:cmd "compile-to-js" :to-chan ch :code the-code}))
      (go (let [res (<! ch)]
            (when (:error res) (js/alert (str (:error res))))
            (when-let 
              [blocks (:js-code res)]
              (js/eval "if (!cljs) cljs = {};\n")
              (js/eval "if (!cljs.user) cljs.user = {};")
              (str (map (fn [x] (do
                                   (js/eval (str x ";")))) blocks))
              (let [tchan cljs.user/twitter
                    res-chan (async/tap (deref cljs.user/averages) (async/chan))]
                
                (in-scope (s-set :sentiment (clj->js {})))
                       
                (send-to-chan tchan (:sample $scope))
                
                (read-from-chan 
                  res-chan 
                  (fn [info]
                    (in-scope (s-set :sentiment (clj->js info)))))
                
                (.log js/console "Done"))))
          )))
  
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