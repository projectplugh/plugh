(ns plugh.visi.parser
  (:require [instaparse.core :as insta]))


(def parse-def
  "
  Lines = (Line (<'\n'>)*)*
  Line = ((BlockComment <'\n'>) | LineComment)* (SINK / Def / Source)
  Def = <START_OF_LINE> (ConstDef | FuncDef);
  ConstDef = IDENTIFIER SPACES <'='> SPACES? EXPRESSION SPACES? <'\n'>;
  FuncDef = IDENTIFIER SPACES? <'('> SPACES? (IDENTIFIER SPACES? <','> SPACES?)* IDENTIFIER SPACES? <','>? SPACES? <')'> SPACES? 
            <'='> EXPRESSION SPACES? <'\n'>;
  SINK = <START_OF_LINE> <'sink:'> SPACES? IDENTIFIER SPACES? <'='> EXPRESSION <'\n'>;
  Source = <START_OF_LINE> <'source:'> SPACES? IDENTIFIER SPACES? <'\n'>;
  START_OF_LINE = #'^' ;
  <SPACES> = (<'\n'> SPACES) / (SPACES <'\n'> SPACES) / (SPACES? LineComment SPACES?) / (SPACES? BlockComment SPACES?)  / <(' ' | '\t')+>
  LineComment = (SPACES? <';;'> (#'[^\n]')*) 
  BlockComment = <'/*'> (BlockComment | (!'*/' AnyChar))* <'*/'>
  <AnyChar> = #'.' | '\n'
  IDENTIFIER = #'[a-zA-Z][a-zA-Z0-9\\-_]*'
  BlockExpression = SPACES? <'begin'> SPACES (EXPRESSION)+ SPACES <'end'> 
  EXPRESSION = (BlockExpression / (SPACES (ConstDef | FuncDef) SPACES EXPRESSION) / 
               IfElseExpr / FuncCall / ParenExpr /  ConstExpr / 
               FieldExpr / FunctionExpr / MapExpr / VectorExpr /
               (SPACES? IDENTIFIER SPACES?)) (SPACES Operator SPACES EXPRESSION SPACES?)?
  FuncCall = SPACES? IDENTIFIER <'('> (EXPRESSION <','>)*  EXPRESSION <','>? SPACES? <')'> SPACES?
  ParenExpr = (SPACES? <'('> SPACES Operator SPACES <')'> SPACES?) |
              (SPACES? <'('> SPACES? EXPRESSION SPACES Operator SPACES <')'> SPACES?) |
              (SPACES? <'('> SPACES Operator SPACES EXPRESSION SPACES? <')'> SPACES?) |
              (SPACES? <'('> EXPRESSION <')'> SPACES?)
  Keyword = <':'> IDENTIFIER 
  IfElseExpr = SPACES? <'if'> SPACES EXPRESSION SPACES <'then'> SPACES EXPRESSION SPACES <'else'> SPACES EXPRESSION
  ConstExpr = SPACES? (Number | Keyword | StringLit) SPACES?
  FieldExpr = SPACES? IDENTIFIER? (SPACES? <'.'> IDENTIFIER)+ SPACES?
  FunctionExpr = SPACES? (IDENTIFIER | (<'('> SPACES? (IDENTIFIER SPACES? <','> SPACES?)*  IDENTIFIER SPACES? <','>? SPACES? <')'> ) ) SPACES? <'=>'> SPACES? EXPRESSION SPACES?
  Number = #'(\\-|\\+)?\\d+' NumberQualifier?
  NumberQualifier = ('%' | ':minutes' | ':hours' | ':seconds' | ':days')
  MapExpr = SPACES? <'{'> (Pair <','>)* Pair (<','> SPACES?)? <'}'> SPACES?
  VectorExpr = SPACES? <'['> (EXPRESSION <','>)* EXPRESSION (<','> SPACES?)? <']'> SPACES?
  Pair = EXPRESSION <'->'> EXPRESSION
  StringLit = <'\"'> ('\\\"' / #'[^\"]')* <'\"'>
  Operator = '+' | '-' | '*' | '/' | '&' | '>' | '<' | '==' | '>=' | '<=' | '&&' | '||' | '<>' | '-?>' | ':=' | ':>='
  " )

(def example "source: twitter

add-sentiment(tweet) = tweet.text -?> calc-sentiment -?> (tweet.sentiment := )

             
with-sentiment = begin xform-some(add-sentiment, twitter) end

             
filtered-sentiment = 
  filter( x => sent = x.sentiment
               ((sent.pos > 2) || 
               (sent.neg < -2)), 
          with-sentiment)

average-sentiment = reduce( (c, t) => c :>= {.pos-sum -> ( + t.sentiment.pos),
                                             .neg-sum -> ( + t.sentiment.neg),
                                             .pos-cnt -> inc,
                                             .neg-cnt -> inc} , 
                           {.pos-sum -> 0, .neg-sum -> 0, 
                            .pos-cnt -> 0, .neg-cnt -> 0} , filtered-sentiment)

             
             
groups = group-by(twitter, ( -?> .date -?> date-to-int -?> ( / 10:minutes )) )

             
sink: buckets = groups

sink: averages = average-sentiment

")


(def the-parser
  (insta/parser parse-def
:start :Lines))

(defn visi-parse [s]
  (let [sp (if (.endsWith s "\n") s (str s "\n"))]
    (the-parser sp))
  )