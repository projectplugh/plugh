(comment (ns plugh.html.parser
  ^{:doc "HTML Parser and such"
    :author "David Pollak"}
  (:use plugh.util.misc
        plugh.util.file)
  
  (:import (nu.validator.htmlparser.sax HtmlParser)
           (nu.validator.htmlparser.common XmlViolationPolicy))
  )

(def ^:dynamic *stack*)
(def ^:dynamic *current*)
(def ^:dynamic *state*) ;; :between :text :cdata :element :comment
(def ^:dynamic *chars*)

(comment 
(def ^{:private true}
  handler 
  (proxy [org.xml.sax.ext.DefaultHandler2] []
    (characters 
      [^chars ch start length] )
    (comment[^chars ch start length])
    (startCDATA [])
    (endCDATA [])
    (startElement [uri local-name q-name ^Attributes atts])
    (endElement [uri local-name q-name])
    ))

(defn html5parse 
  "Parse incoming thing as Html5 using the Validator.nu Html5 parser.
  The parameter can be a String, a File, or an InputStream"
[str]
(let [parser (new HtmlParser XmlViolationPolicy/ALLOW)]
  (. parser setCommentPolicy XmlViolationPolicy.ALLOW)
  (. parser setContentNonXmlCharPolicy XmlViolationPolicy.ALLOW)
  (. parser setContentSpacePolicy XmlViolationPolicy.FATAL)
  (. parser setNamePolicy XmlViolationPolicy.ALLOW)
  
  (binding [*stack* nil
            clojure.xml/*current* {}
            clojure.xml/*state* :between
            clojure.xml/*chars* nil]
    (. parser setContentHandler handler)
    (. parser setLexicalHandler handler)
    (. parser parse str )
    ((:content *current*) 0))))
))
