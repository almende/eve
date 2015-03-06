---
layout: default
title: News
---

#Eve - news

## March 6th, 2015: Eve Java 3.1 *Flying Dutchman* released!

Somewhat like it's namesake ship, this latest Eve Java release has appeared unexpectedly on the horizon. Building onto and maturing the 3.X branch, this release contains many small enhancements and some big infrastructure additions:

- The JSON-RPC protocol has been generalized into a protocol stack, allowing runtime switching of communication protocol and inserting utility conversions (e.g. compression, encryption, tracing, etc.)
- The bootstrapping & agent destruction infrastructure has been enhanced to closer match 2.X and allow more complex setups as well.
- Various new capabilities and tools have been added:
	- A Redis state
	- A time-synchronizing scheduler, allowing agents in multiple runtime environments to coordinate their timing at millisecond range.  Check out the: [Musical demo](http://youtu.be/bGoe3CiQjOU)
	- A distributed addition algorithm has been provided.
	- An implementation of the [Trickle (RFC6206)](https://tools.ietf.org/html/rfc6206) protocol has been provided.

There have been many non-visible enhancements as well, mostly providing even better performance, error handling and robustness. The performance test, mentioned at the 3.0 release, now shows over 800k RPC calls per second on a 8-core notebook.

The 3.X branch has now been used in various European research projects, proving to be an excellent base for further Eve development. Especially in the realms of multi-agent algorithms, many enhancements and future developments are planned. The Flying Dutchman release is a steppingstone in providing more maturity for the 3.X branch. This ghost ship may only be passing quietly in the night, but is guaranteed to stay afloat forever:)

## September 26th, 2014: Eve JS 0.4.3 release!

Stadily working towards a fully featured, mature implementation of Eve in Javascript, we have release version 0.4.3 of evejs. 
Eve JS is part of the Enmasse.io framework, a toolset of agent capabilities in Javascript.

You can read more about this version on this website and at the [Enmasse.io](https://github.com/enmasseio/evejs) repository. 

## July 11th, 2014: Eve 3.0 *Endeavour* released!

After a two months focussed effort we have release Eve 3.0, the "Endeavour". Eve 3.0 represents a major rewrite of Eve, achieving a remarkable level of flexibility in designing agents.

Her namesake ship, which carried James Cook from South-America to Australia and New Zealand, featured a special shallow keel, allowing the ship to be beached for repairs without a dry-dock. Combined with the ability to navigate shallow waters, this gave her the necessary robustness and flexibility vital to the travels into the unknown. 

Where Eve 2.x was designed around a host providing services to the agents, Eve 3.0 takes a more bottom-up approach: There is a library of agent capabilities which can be added to any Java class to form an agent. A capability is self-contained and shares as little as possible with other capabilities. Combined with a very flexible graph-based configuration system(Json DOM), this allows Eve to be used in a large variety of scenarios:

- Adding agent capabilities to legacy java code
- Minimize resource usage for embedded java setups
- Scale up for large cloud setups
- Through the new Websocket transport support (=a viable alternative for XMPP) the mobile agent setup gets better scalable and flexible
- Adding new capabilities is now very easy, which will allow Eve to provide simulations as well: adding a "simulation scheduler" is in planning...

One very noticeable change in agent behaviour compared to earlier versions is the lifecycle of an agent instance. An agent instance now stays in memory by default, with behavior much closer to the normal expected Java object lifecycle. For large scale, low-latency setups, it is still advisable to use the request triggered instance lifecycle, which remains fully supported through the "Wakeable agent" model.

Last but not least, Eve 3.0 provides another significant performance enhancement. In our baseline performance test, based on the Game-of-Life demo, Eve 3.0 gains another 2-3x higher amount of RPC calls per second, compared with Eve 2.X. This, combined with a lines-of-code reduction of 30% and a 20% drop in cyclic-redundancy complexity, makes Eve 3.0 a very capable vessel, like Cook's Endeavour before her.

The online documentation has been updated and extended: http://eve.almende.com
As always, any questions, issues, discussions and ideas can be communicated through Github issues: https://github.com/almende/eve/issues

## April 2th, 2014: Eve 2.2.2 bugfix release provided

Thanks to testing and usage of Eve at ASK and RCS, we've been able to fix bugs in the 2.2 branch. Most significant fixes are in the AgentProxy and reloading behavior of the agent host.


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

