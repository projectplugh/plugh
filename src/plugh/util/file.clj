(ns plugh.util.file
  (:gen-class)
  (:use [clojure.java.io])
  )

(defn each-file [f]
  "takes a file and returns a list of files if the root file is a directory"
  (let [rf (file f)]
    (cond
      (.isDirectory rf) (mapcat each-file (.listFiles rf))
      (.isFile rf) [rf]
      :else ()
      )))

(defn map-file [the-file func]
  "applies a function to each file in a directory"
  (map func (each-file the-file)))

(defn file-to-bytes [the-file]
  "Read a file or input stream into bytes"
  (io!
    (let [ba (byte-array 4096)]
      (with-open [is (input-stream the-file)
                  os (new java.io.ByteArrayOutputStream)]
        (loop [len (. is read ba)]
          (if (< len 0)
            (. os toByteArray)
            (do
              (if (> len 0) (. os write ba 0 len))
              (recur (. is read ba))
            )))))))

(defn file-to-string [the-file]
  "read a file as a UTF-8 encoded string"
  (new String (file-to-bytes the-file) "UTF-8"))