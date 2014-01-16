---
layout: default
title: Downloads
---

{% assign version = '2.2.0' %}


# Downloads

The Eve libraries for java are available in the
[Maven Central Repository](http://search.maven.org/#search%7Cga%7C1%7Ceve).
All libraries are licensed under the [Apache License, version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).

## Maven

To add the eve library to a maven project, add the following to the projects
pom.xml file. Only eve-core is required, the others depend on the environment in which Eve is used.

### eve-core

Core library.

{% highlight xml %}
<dependency>
    <groupId>com.almende.eve</groupId>
    <artifactId>eve-core</artifactId>
    <version>{{version}}</version>
</dependency>
{% endhighlight %}

### eve-gae

Google App Engine support.

{% highlight xml %}
<dependency>
    <groupId>com.almende.eve</groupId>
    <artifactId>eve-gae</artifactId>
    <version>{{version}}</version>
</dependency>
{% endhighlight %}

### eve-android

Android support.

{% highlight xml %}
<dependency>
    <groupId>com.almende.eve</groupId>
    <artifactId>eve-android</artifactId>
    <version>{{version}}</version>
</dependency>
{% endhighlight %}

### eve-planning

Example application on top of Eve:

{% highlight xml %}
<dependency>
    <groupId>com.almende.eve</groupId>
    <artifactId>eve-gae</artifactId>
    <version>{{version}}</version>
</dependency>
{% endhighlight %}


## Manual download

Instead of letting maven resolve all library dependencies, it is possible to manually download
all jars.

### eve-core

Dependency tree of eve-core:

- [eve-core-{{version}}.jar](http://search.maven.org/#search|ga|1|eve-core)
- [jackson-databind-2.2.1.jar](http://search.maven.org/#search|ga|1|jackson-databind)
  - [jackson-annotations-2.2.1.jar](http://search.maven.org/#search|ga|1|jackson-annotations)
  - [jackson-core-2.2.1.jar](http://search.maven.org/#search|ga|1|jackson-core)
- [jackson-datatype-joda-2.2.1.jar](http://search.maven.org/#search|ga|1|jackson-datatype-joda)
  - [jackson-annotations-2.2.1.jar](http://search.maven.org/#search|ga|1|jackson-annotations)
  - [jackson-core-2.2.1.jar](http://search.maven.org/#search|ga|1|jackson-core)
  - [joda-time-2.1.jar](http://search.maven.org/#search|ga|1|joda-time)
- [joda-time-2.1.jar](http://search.maven.org/#search|ga|1|joda-time)
- [snakeyaml-1.11.jar](http://search.maven.org/#search|ga|1|snakeyaml)
- [smackx-3.1.0.jar](http://search.maven.org/#search|ga|1|smackx) (optional, only needed for XMPP support)

  - [smack-3.1.0.jar](http://search.maven.org/#search|ga|1|smack)

- [httpclient-4.2.3.jar](http://search.maven.org/#search|ga|1|httpclient)
  - [httpcore-4.2.2.jar](http://search.maven.org/#search|ga|1|httpcore)
  - [commons-logging-1.1.1.jar](http://search.maven.org/#search|ga|1|commons-logging)
  - [commons-codec-1.6.jar](http://search.maven.org/#search|ga|1|commons-codec)
- [typetools-0.3.0.jar](http://search.maven.org/#search|ga|1|typetools)

### eve-gae

The Google App Engine library for eve has the following dependencies:

- [eve-gae-{{version}}.jar](http://search.maven.org/#search|ga|1|eve-gae)

  - [eve-core-{{version}}.jar](http://search.maven.org/#search|ga|1|eve-core)

    - all dependencies listed at eve-core

  - [twig-persist-2.0-rc.jar](https://code.google.com/p/twig-persist/)

    - [guava-10.0.jar](http://search.maven.org/#search|ga|1|guava)

      - [jsr305-1.3.9.jar](http://search.maven.org/#search|ga|1|jsr305)

    - [guice-3.0.jar](http://search.maven.org/#search|ga|1|guice)

      - [javax.inject-1.jar](http://search.maven.org/#search|ga|1|javax.inject)

      - [aopalliance-1.0.jar](http://search.maven.org/#search|ga|1|aopalliance)


### eve-android

Eve Android has the following dependencies:

- [eve-android-{{version}}.jar](http://search.maven.org/#search|ga|1|eve-android)

  - all dependencies listed at eve-core

- [asmack-android-7.jar](https://github.com/almende/eve/tree/master/java/misc)


### eve-planning

Eve Planning has the following dependencies:

- [eve-planning-{{version}}.jar](http://search.maven.org/#search|ga|1|eve-planning)

  - [eve-core-{{version}}.jar](http://search.maven.org/#search|ga|1|eve-core)

    - all dependencies listed at eve-core
