Handles 2 different types of logs:
* single line (syslog)
* multi-line (log4j, stack traces)
  * these might require a template to be written that explains where certain types of data are; making java & ruby stacktraces searchable can be incredibly useful

Provides a real-time-ish dashboard that displays (storm + incanter?):
* summary stats for number-like things in single line items
* mean, median, quintiles, 90%tile, 99%tile, 1st deriv, 2d deriv
* allows grouping + sorting by any column

* Separate display for multi-line items which are aggregatable by hostnames, ip addresses, applications, line number, file, error string, &c -- the problem with these is that they’re so application specific that I can almost guarantee that we’ll need to provide some type of config file and/or DSL that lets people minimally describe what kind of data they expect in a multi-line logfile

Indexes incoming data for simple searches:
* search for strings
* basic predicates “BLAH < X” “NOT in (ABC, DEF)”
* provides derived columns by application of well-defined functions (AVG, SUM, &c) (arbitrary functions to come later)
* search needs to understand IP addresses (10.0.1.0/32 is inside of 10.0.1.0/24)
