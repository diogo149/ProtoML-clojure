ProtoML-clojure
===============

clojure implementation of ProtoML's ideas - for fun!

For testing server:
    $ cd sample
    $ lein ring server

For testing client:
    $ cd sample
    $ curl -XPOST localhost:3000/new-transform -d @sample-request.txt

General TODOs
=============
* tests
* fill in sample files
* make transforms
* find out how to do io tests easily?

Notes
=====
*Items in a request cannot contain an ampersand (&).

generate output prefixes as well
make file formatter first class
