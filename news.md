---
layout: default
title: News
---

#Eve - news

## January 17th, 2014: Eve 2.2.0 *Dreadnought* released!

The next ship of the line for Eve has cruised into open seas: Eve 2.2.0 *Dreadnought* has been released and is available through Maven\'s central repository. This is a Java version release of Eve.

Eve 2.2 brings major enhancements in the transport layer: 

- An 8x performance enhancement in basic RPC call handling (upto handling 100.000 RPC calls per second (yeh!))
- The introduction of the ZMQ transport protocol
- Eve is now *stream-ready*, setting the stage for introducing a streaming protocol besides the JSON-RPC protocol. This effort required a clear separation between the transport layer and the RPC protocol, which has had the added benefit of a reduction in complexity of Eve.

But it also introduces various fixes at other places:

- Sven (of ASK CS) donated a CouchDB state implementation! Thanks!
- Exception handling is much more consistent through the services.
- Through the agents signal handlers it is now much easier to provide full agent tracing/logging.
- Method naming has been made more consistent as well: 
- Agent signal handlers are now called *on{Signal}* (e.g. onInit, onDelete)
- @Required(false) is now being replaced by @Optional
- There have been some major steps in the ongoing effort to make Eve more extensible in the *standard Java* way, with smarter scoping, steps towards better injection pattern designs, etc.

As always, if the coming period leads to questions, potential bug reports, and/or general support questions, please don\'t hesitate to contact me or Jos. Where possible please use the github issue list for this purpose!

Basic links: 

- [http://eve.almende.com](http://eve.almende.com)
- [https://github.com/almende/eve](https://github.com/almende/eve)
- [changelog.txt](https://github.com/almende/eve/blob/master/java/eve-core/changelog.txt)

On a separate important note: Besides this work on the Java version of Eve, several people are working on the Javascript implementation as well. You can expect news on that front in the near future as well!

