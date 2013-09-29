ProtoML-clojure
===============

clojure implementation of ProtoML's ideas - for fun!

For testing server:
    $ cd sample
    $ lein ring server

For testing client:
    $ cd sample
    $ curl -XPOST localhost:3000/new-transform -d @sample-request.txt

Unfinished
==========
*Validation
*Auto file formatter
*Input data
*Package transform (for metatransforms)

General TODOs
=============
* tests
* fill in sample files
* make transforms
* split io file into io and pipeline

Notes
=====
*Items in a request cannot contain an ampersand (&).
*Transform parameters should not contain an equal sign (=).

Transform Definition
====================
Required fields
* Documentation
* Parameters
* Input: Array/list of:
    * Type
    * Extension (including the period)
    * NCols (have -1 for any, 0 for none, positive number for a specific number)
* Output
    * Type
    * Extension (including the period)
    * NCols (have -1 for not specified, 0 for none, positive number for a specific number)
