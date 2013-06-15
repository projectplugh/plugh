(defproject plugh "0.1.0-SNAPSHOT"
  :description "An open source log analysis tool"
  :url "http://plugh.im"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.novemberain/welle "1.5.0" :exclude [org.apache.httpcomponents/httpclient]]
                 [joda-time/joda-time "2.2"]
                 [aleph "0.3.0-rc1"]
                 [org.clojure/core.match "0.2.0-alpha12"]
                 [org.clojure/clojurescript "0.0-1820"]
                 [com.google.javascript/closure-compiler "r1592"]
                 [org.clojure/google-closure-library "0.0-790"]
                 [org.apache.httpcomponents/httpclient "4.2.5"]
                 ]
  ; :jvm-opts ["-Xmx512M"]
  ;; Control the context in which your project code is evaluated.
  ;; Defaults to :subprocess, but can also be :leiningen (for plugins)
  ;; or :classloader (experimental) to avoid starting a subprocess.
  ; :eval-in :classloader
  :main plugh.core)
