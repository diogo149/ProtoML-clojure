ProtoML-clojure
===============

clojure implementation of ProtoML's ideas - for fun!

For testing server:

> cd sample

> lein ring server

For testing client:

> cd sample

> curl -XPOST localhost:3000/manual-input -d @sample-manual-input.txt

> curl -XPOST localhost:3000/new-transform -d @sample-new-transform.txt

For testing ElasticSearch:

> curl -XGET 'http://localhost:9200/protoml/new-transform/_search?pretty=true'

> curl -XGET 'http://localhost:9200/protoml/new-transform/_search?q=model-path:data&pretty=true'

To Do List
==========
* Elastic Search integration
* More tests
* Make transforms
* Command line client
* Validation
* Auto file formatter
* Transform package/partial application (for metatransforms)

Notes
=====
* Items in a request cannot contain an ampersand (&).
* Transform parameters should not contain an equal sign (=).

Transform Definition
====================
Required fields
* Documentation
* Parameters
* Input: Array/list of:
    * Type
    * Extension (including the period)
    * NCols (have -1 for any, 0 for none, positive number for a specific number)
* Output Array/list of:
    * Type
    * Extension (including the period)
    * NCols (have -1 for not specified, 0 for none, positive number for a specific number)
