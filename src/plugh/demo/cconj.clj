(ns plugh.demo.cconj
  (:require [clojurewerkz.welle.core :as wc]
            [clojurewerkz.welle.buckets :as wb]
            [plugh.visi.parser :as visi]
            [clojure.core.async :as async]))

;; connect to Riak
(def riak-thingy (wc/connect!))

(def code "source: twitter

add-sentiment(tweet) = tweet.text >> calc-sentiment >> (tweet.sentiment := )

with-sentiment = xform(add-sentiment, twitter)

filtered-sentiment =
  xfilter( x => sent = x.sentiment
                ((sent.pos > 1) ||
                (sent.neg < -1)),
          with-sentiment)

average-sentiment = flow( (c, t) => c :>= {.pos-sum -> ( + t.sentiment.pos),
                                           .neg-sum -> ( + t.sentiment.neg),
                                           .pos-cnt -> inc,
                                           .neg-cnt -> inc} ,
                           {.pos-sum -> 0, .neg-sum -> 0,
                            .pos-cnt -> 0, .neg-cnt -> 0} , filtered-sentiment)

                            
sink: averages = average-sentiment")

(def sample-data 
  '({:text
    "RT @BlogforIowa: Rand Paul-Mitch McConnell caught on mic talking #shutdown strategy video removed from Youtube  http://t.co/hbd8lGS6vM",
    :id 386496574391463936,
    :created_at "Sat Oct 05 14:22:18 +0000 2013"}
   {:text
    "RT @jiya043: Government #shutdown Memes: funny because they are true; especially #9 #Obamacare &gt;&gt;&gt; http://t.co/rKXk7ZwvVT - sp",
    :id 386496587800653824,
    :created_at "Sat Oct 05 14:22:21 +0000 2013"}
   {:text
    "Tell Obama and Harry Reid to end the #shutdown, stop hurting #veterans, and #stopobamacare.  http://t.co/In8txodW6w via @Heritage_Action",
    :id 386496593454592000,
    :created_at "Sat Oct 05 14:22:23 +0000 2013"}
   {:text
    "RT @thejendra: BUY NOW - How to think like a Coward - http://t.co/dvMOajucjv #mustread #selfimprovement #amazon #nonfiction #paperback #iBo…",
    :id 386496596374204416,
    :created_at "Sat Oct 05 14:22:24 +0000 2013"}
   {:text
    "#SHUTDOWN pay leg PERFECT ex of why BIG CENTRALIZED GOVT SO BAD. Reps voting bc suffering is UP CLOSE &amp; AFFECTS THEM PERSONALLY. #TeaParty",
    :id 386496601272770560,
    :created_at "Sat Oct 05 14:22:25 +0000 2013"}
   {:text
    "RT @STRONG_OPED: #SHUTDOWN  IN UNION TERMS THIS IS A LOCK OUT NOT A SHUT DOWN.\n\nOBAMA TO CANCEL CHRISTMAS IF HE DOESN'T GET HIS WAY.\n@FRANC…",
    :id 386496602979848192,
    :created_at "Sat Oct 05 14:22:25 +0000 2013"}
   {:text
    "#Obama et les Républicains, ensemble pour l’austérité et le #krach http://t.co/891DLiDAzS #shutdown",
    :id 386496603692879874,
    :created_at "Sat Oct 05 14:22:25 +0000 2013"}
   {:text
    "RT @SpeakerBoehner: WH says govt #shutdown “doesn’t really matter” http://t.co/MyjWJmVUIO GOP continues taking action to keep critical part…",
    :id 386496611284971520,
    :created_at "Sat Oct 05 14:22:27 +0000 2013"}
   {:text
    "RT @SteveWorks4You: Obama is intentionally making the #shutdown as long and traumatic as possible because he wants politicians to fear any …",
    :id 386496611678814208,
    :created_at "Sat Oct 05 14:22:27 +0000 2013"}
   {:text
    "RT @rtoberl: Why would @SenatorReid want to help one child with cancer?  He wouldn't. #shutdown http://t.co/Vc6OgzHbGJ via @Heritage_Action",
    :id 386496618402299904,
    :created_at "Sat Oct 05 14:22:29 +0000 2013"}
   {:text
    "RT @DonnyFerguson: Democrats are about to vote to make it illegal for military chaplains to minister during the #shutdown. How screwed up i…",
    :id 386496619262119936,
    :created_at "Sat Oct 05 14:22:29 +0000 2013"}
   {:text
    "RT @USATODAY: Here's where the current #shutdown stands: http://t.co/YI8NQ7s5wB",
    :id 386496620801441793,
    :created_at "Sat Oct 05 14:22:29 +0000 2013"}
   {:text
    "RT @SteveWorks4You: GOP has passed seven bills to get government re-opened. Obama has ordered them killed and #shutdown extended. http://t.…",
    :id 386496620860145664,
    :created_at "Sat Oct 05 14:22:29 +0000 2013"}
   {:text
    "#shutdown #stabenow Due to the Govt shutdown, E-mails &amp; letters will not be delivered to us and offices are closed until the shutdown is ove",
    :id 386496624672788481,
    :created_at "Sat Oct 05 14:22:30 +0000 2013"}
   {:text
    "RT @Joe_Covey: #Shutdown RT @amandacarpenter: Want a list of all the bills the House has passed to fund government? Here you go http://t.co…",
    :id 386496626937692160,
    :created_at "Sat Oct 05 14:22:31 +0000 2013"}
   {:text
    "RT @USATODAY: Here's where the current #shutdown stands: http://t.co/YI8NQ7s5wB",
    :id 386496635057876992,
    :created_at "Sat Oct 05 14:22:33 +0000 2013"}
   {:text
    "RT @Forbes: What caused the U.S. government #shutdown? http://t.co/GTPTG7TJfu",
    :id 386496644633477120,
    :created_at "Sat Oct 05 14:22:35 +0000 2013"}
   {:text
    "RT @USATODAY: Here's where the current #shutdown stands: http://t.co/YI8NQ7s5wB",
    :id 386496651679899648,
    :created_at "Sat Oct 05 14:22:37 +0000 2013"}
   {:text
    "RT @Garrett_R_Hall: Many of our National Parks actually make money. So, naturally you'd want to close those immediately during a #Shutdown.…",
    :id 386496666116685826,
    :created_at "Sat Oct 05 14:22:40 +0000 2013"}
   {:text
    "#Shutdown: A sense of unease is growing in global capitals as the US govt from afar looks increasingly befuddled http://t.co/lYYWSw7iPK",
    :id 386496680679731200,
    :created_at "Sat Oct 05 14:22:44 +0000 2013"}
   {:text
    "RT @USATODAY: Here's where the current #shutdown stands: http://t.co/YI8NQ7s5wB",
    :id 386496682600706048,
    :created_at "Sat Oct 05 14:22:44 +0000 2013"}
   {:text
    "Hey #vets... here's a robust FAQ and A page about benefits and other things affected by the #shutdown. http://t.co/qTkdN6olX0",
    :id 386496682914881536,
    :created_at "Sat Oct 05 14:22:44 +0000 2013"}
   {:text
    "@CNN @JohnKerry so this #shutdown is a game played by the #senate?",
    :id 386496704104509441,
    :created_at "Sat Oct 05 14:22:49 +0000 2013"}
   {:text
    "@RepSwalwell quit being a GOVT. Stooge. We don't want BIG GOVT controlling our lives. What do you NOT get about that?! #shutdown",
    :id 386496716892954624,
    :created_at "Sat Oct 05 14:22:52 +0000 2013"}
   {:text
    "RT @USATODAY: Here's where the current #shutdown stands: http://t.co/YI8NQ7s5wB",
    :id 386496721465147392,
    :created_at "Sat Oct 05 14:22:53 +0000 2013"}
   {:text
    "Morning #nerdland. Of course #shutdown is absurd. Ppl it hurts are invisible 2 GOTPers. WIC &amp; SNAP recipients don't contrib 2 or vote 4 GOP",
    :id 386496721464737793,
    :created_at "Sat Oct 05 14:22:53 +0000 2013"}
   {:text "RT @BarackObama: Retweet if you want this #shutdown to end.",
    :id 386496722215518208,
    :created_at "Sat Oct 05 14:22:54 +0000 2013"}
   {:text
    "RT @NPCA: According to #shutdown impact counter, communities around #nationalparks have lost over $131 million. http://t.co/nFcsa0cXYH #Kee…",
    :id 386496732780969984,
    :created_at "Sat Oct 05 14:22:56 +0000 2013"}
   {:text
    "Furloughed Texan Cruz supporters to America: 'You've got to stand up' http://t.co/ybdZdVNDS5 #shutdown",
    :id 386496740926713856,
    :created_at "Sat Oct 05 14:22:58 +0000 2013"}
   {:text
    "RT @iowahawkblog: Oh My Government: military chaplains threatened with arrest if they conduct services during #Shutdown http://t.co/oSK8tzI…",
    :id 386496746375094272,
    :created_at "Sat Oct 05 14:22:59 +0000 2013"}
   {:text
    "America's government #shutdown: No #way to run a #country | The Economist http://t.co/IImP79DY5G",
    :id 386496756944367616,
    :created_at "Sat Oct 05 14:23:02 +0000 2013"}
   {:text
    ".@EBONYMag RT @Lumumbabandele: @KBDPHD makes sure we \"stay woke\"  Kids &amp; Families:The REAL Victims of  #Shutdown - http://t.co/NEZGzPk7PM",
    :id 386496761671737344,
    :created_at "Sat Oct 05 14:23:03 +0000 2013"}
   {:text
    "RT @BarackObama: Here's how the #shutdown is hurting every single state: http://t.co/AU2vBMW4zT #EnoughAlready",
    :id 386496762644410368,
    :created_at "Sat Oct 05 14:23:03 +0000 2013"}
   {:text
    "RT @WithoutViolence: #DVAM - survivors across the US face uncertainty as the gov #shutdown threatens vital programs &amp; resources http://t.co…",
    :id 386496763441324032,
    :created_at "Sat Oct 05 14:23:03 +0000 2013"}
   {:text
    "@MoveTrue\"Essential workers are working without pay.\" Via @MHPshow #shutdown #nerdland",
    :id 386496767887294465,
    :created_at "Sat Oct 05 14:23:04 +0000 2013"}
   {:text
    "Donohue humor is the best humor. #shutdown #congress http://t.co/xPaNaWuqfJ",
    :id 386496770999848960,
    :created_at "Sat Oct 05 14:23:05 +0000 2013"}
   {:text
    "#Shutdown log, day 5.\nWith no money to train drivers, UGA busses are now literally allowed to run you over and crash into things. Legally.",
    :id 386496771662557185,
    :created_at "Sat Oct 05 14:23:05 +0000 2013"}
   {:text "Mr. Smith Goes To #Shutdown Washington\n#AddAWordRuinAMovie",
    :id 386496780890013696,
    :created_at "Sat Oct 05 14:23:07 +0000 2013"}
   {:text
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
    :created_at "Sat Oct 05 14:23:33 +0000 2013"}))

(defn send-demo-data-to [the-code target-chan]
  (println "messing up " the-code)
  (let [res (visi/visi-compile (visi/visi-parse the-code) 'frog.dog false)
        the-ns (find-ns (:name-space res))
        twitter (deref (ns-resolve the-ns (symbol "twitter")))
        ]
    (dorun (map (fn [sink]
                  (let [the-chan (async/tap (deref (deref (ns-resolve the-ns (symbol sink)))) (async/chan))]
                    (async/pipe the-chan target-chan)))
                (:sinks res)))
    (dorun (map (fn [d] (do (java.lang.Thread/sleep 100 ) (async/>!! twitter d))) sample-data))
    ;; (async/close! twitter)
    ))


(defn printer-chan [] 
  (let [ch (async/chan)]
    (async/go-loop 
      []
      (let [v (async/<! ch)]
        (if (not v) []
          (do
            (println "Value from channel " v)
            (recur)))
        ))
    ch))

(defn test-demo []
  (send-demo-data-to code (printer-chan))
  "Done with test-demo, dude!")


;; EOF