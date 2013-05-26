(defproject plugh "0.1.0-SNAPSHOT"
  :description "An open source log analysis tool"
  :url "http://plugh.im"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.novemberain/welle "1.5.0"]
                 [aleph "0.3.0-rc1"]
                 [org.clojure/core.match "0.2.0-alpha12"]
                 ]

  :main plugh.core)
