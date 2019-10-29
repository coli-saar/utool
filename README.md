![Utool Logo](doc/utool.png)

Utool is the Swiss Army Knife of Underspecification. It is a GUI and library written in Java for performing computations with dominance graphs and other formalisms, which are used to represent semantic ambiguities in natural language processing.

![Utool Screenshot](doc/ubench-screenshot.png)

You can find [detailed documentation](http://www.coli.uni-saarland.de/projects/chorus/utool/page.php?id=manual) on the [Utool homepage](http://www.coli.uni-saarland.de/projects/chorus/utool).

Utool was developed in 2005-2010 in the CHORUS Project at  [Saarland University](https://www.lst.uni-saarland.de/). It is no longer under active development, but it is probably still the fastest solver for underspecified representations of scope ambiguities, and will still run fine today. If you have any questions or requests, please get in touch with [Alexander Koller](http://www.coli.uni-saarland.de/~koller/).

You can always download the most recent release of Utool from the [Releases page](https://github.com/coli-saar/utool/releases).

## Compiling Utool

To compile Utool, you will need a recent version of [Apache Maven](https://maven.apache.org/) and [Java JDK 8](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html). Notice that Utool currently won't compile with more recent versions of Java.

Clone Utool from Github and the compile as follows:

```
mvn install assembly:single
```

This will produce a file `target/utool-<version>-jar-with-dependencies.jar`, where `<version>` is the version of Utool. We will call this file `utool.jar` below for simplicity.


## Running Utool

Run Utool as follows to get some elementary help on command-line usage:

```
java -jar utool.jar
```

You can open the GUI shown above as follows:

```
java -jar target/utool.jar display
```

Note that you need Java 8 (not higher) to *run* Utool too.

