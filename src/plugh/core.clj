(ns plugh.core
  (:use plugh.http.server)

  (:gen-class )
  )

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "dog")
  (run-server 8080)
  (println "Hello, World!"))
