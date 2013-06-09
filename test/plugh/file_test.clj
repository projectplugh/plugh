(ns plugh.file-test
  (:use clojure.test
        plugh.util.file))

(deftest each-file-test
  (is
    (not
      (empty?
        (filter
          (fn [f]
            (. (. f getName) endsWith "md"))
          (each-file "."))))))

(deftest read-string-test
  (is
    (>
      (. (file-to-string "./lein")
         lastIndexOf "TRAMP")
      5000)))

