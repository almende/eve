---
layout: default
title: Downloads
---

{% assign version = '1.1.0' %}


# Downloads

The Eve libraries for java are available in the
[Maven Central Repository](http://search.maven.org/#search%7Cga%7C1%7Ceve).
All libraries are licensed under the [Apache License, version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).

## Maven

To add the eve library to a maven project, add the following to the projects
pom.xml file. Only eve-core is required, the others depend on the environment in which Eve is used.

### eve-core

Core library.

    <dependency>
        <groupId>com.almende.eve</groupId>
        <artifactId>eve-core</artifactId>
        <version>{{version}}</version>
    </dependency>

### eve-gae

Google App Engine support.

    <dependency>
        <groupId>com.almende.eve</groupId>
        <artifactId>eve-gae</artifactId>
        <version>{{version}}</version>
    </dependency>

### eve-android

Android support.

    <dependency>
        <groupId>com.almende.eve</groupId>
        <artifactId>eve-android</artifactId>
        <version>{{version}}</version>
    </dependency>

### eve-planning

Example application on top of Eve:

    <dependency>
        <groupId>com.almende.eve</groupId>
        <artifactId>eve-gae</artifactId>
        <version>{{version}}</version>
    </dependency>

## Manual download

The jar files can be downloaded manually from the maven repository:

[http://search.maven.org/#search%7Cga%7C1%7Ceve](http://search.maven.org/#search%7Cga%7C1%7Ceve)

As Eve offers access to many standard services, there are quite some external dependencies:

- [commons-codec-1.6.jar](http://commons.apache.org/proper/commons-codec/)
- [commons-logging-1.1.1.jar](http://commons.apache.org/proper/commons-logging/)
- [httpclient-4.2.3.jar](http://hc.apache.org/downloads.cgi)
- [httpcore-4.2.2.jar](http://hc.apache.org/downloads.cgi)
- [jackson-databind-2.2.1.jar](http://jackson.codehaus.org)
- [jackson-core-2.2.1.jar](http://jackson.codehaus.org)
- [jackson-annotations-2.2.1.jar](http://jackson.codehaus.org)
- [joda-time-2.1.jar](http://joda-time.sourceforge.net/)
- [smack-3.1.0.jar](http://www.igniterealtime.org/projects/smack/)
   (optional, only needed for XMPP support)
- [smackx-3.1.0.jar](http://www.igniterealtime.org/projects/smack/)
   (optional, only needed for XMPP support)
- [snakeyaml-1.11.jar](http://snakeyaml.org)

For the Google App Engine version there are some more dependencies:
  
- [twig-persist-2.0-rc.jar](http://code.google.com/p/twig-persist)
- [guava-10.0.jar](http://code.google.com/p/guava-libraries)
- [guice-3.0.jar](https://code.google.com/p/google-guice/wiki/Guice30)

