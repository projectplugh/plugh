(ns plugh.core
  (:require [plugh.http.server :as ps])

  (:gen-class )
  )


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (ps/start-server 9898)
  (println "Hello, World!"))

