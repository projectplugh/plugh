(ns plugh.visi.parser
  (:require [instaparse.core :as insta]))


(def parse-def
  "
  Lines = (Line (<'\n'>)*)*
  Line = SINK / Def / Source
  Def = <START_OF_LINE> IDENTIFIER (<SPACES> IDENTIFIER)* <SPACES> <'='> EXPRESSION <'\n'>;
  SINK = <START_OF_LINE> <'\"'> IDENTIFIER <'\"'> <SPACES> <'='> EXPRESSION <'\n'>;
  Source = <START_OF_LINE> <'?'> IDENTIFIER <SPACES> <'\n'>;
  START_OF_LINE = #'^' ;
  SPACES = (' ' | '\t')*
  IDENTIFIER = #'[a-zA-Z]+'
  EXPRESSION = (IfElseExpr / FuncCall / ParenExpr /  ConstExpr / (<SPACES> IDENTIFIER <SPACES>)) (<SPACES> Operator <SPACES> EXPRESSION <SPACES>)?
  FuncCall = <SPACES> IDENTIFIER (<SPACES> EXPRESSION <SPACES>)+
  ParenExpr = (<SPACES> <'('> <SPACES> Operator <SPACES> <')'> <SPACES>) |
              (<SPACES> <'('> <SPACES> EXPRESSION <SPACES> Operator <SPACES> <')'> <SPACES>) |
              (<SPACES> <'('> <SPACES> EXPRESSION <SPACES> Operator <SPACES> EXPRESSION <SPACES> <')'> <SPACES>) |
              (<SPACES> <'('> <SPACES> EXPRESSION <SPACES> <')'> <SPACES>)
  IfElseExpr = <SPACES> <'if'> <SPACES> EXPRESSION <SPACES> <'then'> <SPACES> EXPRESSION <SPACES> <'else'> <SPACES> EXPRESSION
  ConstExpr = <SPACES> (Number | StringLit) <SPACES>
  Number = #'\\d+'
  StringLit = <'\"'> #'[^\"]*' <'\"'>
  Operator = '+' | '-' | '*' | '/' | '&' | '>' | '<' | '==' | '>=' | '<='
  " )

(def the-parser
  (insta/parser parse-def
:start :Lines))

(defn visi-parse [s]
  (the-parser s)
  )