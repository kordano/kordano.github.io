---
layout: post
title: Database replication with dat
introduction: A simple peer-to-peer replication setup for datahike
tags: [clojure, datahike, dat, replication]
picture: true
---

With requirements for [offline](http://offlinefirst.org/) capabilities and fault tolerance modern database management systems thrive for mechanisms that support simple replications. By using decentralized [peer-to-peer systems](https://ieeexplore.ieee.org/document/990434/) and by replicating only files we can easily achieve a simple synchronization solution without tempering with databases itself. Obviously as a downside we don't have any conflict resolution if data diverges on network failures.

The following post is an attempt to show database replication as with few steps as possible. Indepth information about the underlying database and synchronization platform are not described here, but useful pointers for a start are given. All in all this is a very technical post, so one should be familiar with basic programming techniques. <!-- Despite a recent upcoming of [language irritations](https://bugs.python.org/issue34605) in the Python community against master/slave terminology we are using this to distinguish the different databases. -->

Although the base idea is very simple it can only be adapted with databases that meet certain criteria. Firstly data updates must be operated atomically on a single file, so that the peer-to-peer system can propagate individual updates. Secondly most database systems use memory mapped files and mutate data randomly. This way we don't get any efficient data deltas and the peer-to-peer system has to calculate the changes at high costs. Luckily we have developed a database that meets the requirements for this use case: datahike. As peer-to-peer solution we choose the dat project with its simple API and open protocol.

The following figure [FIG.1](#FIG.1) illustrates the connection between the database, the peers and the synchronization layer. Updates within the database in process *db1* are written to the filesystem on peer *p1*, the synchronization process *sync1* detects changes and propagates them to other peers where process *sync2* receives changes and applies them to the filesystem of peer *p2*. Then the database process *db2* on that peer *p2* can read the newly received data and perform queries.

{% include image.html url="/resources/images/dat-datahike-model.svg" enum="FIG.1" description="Demo application data flow" %}

# datahike and dat project

[datahike](https://github.com/replikativ/datahike) is a triple store combining a fork of [datascript](https://github.com/tonsky/datascript) and [hitchhiker tree](https://github.com/datacrypt-project/hitchhiker-tree). Datascript is an immutable in-memory database created by [Nikita Prokopov](http://tonsky.me/) that provides a powerful [datalog](https://en.wikipedia.org/wiki/Datalog) query engine and implements an almost complete subset of the API of [datomic](https://www.datomic.com), which is a popular, commercial, transactional database within the Clojure community. Unfortunately datascript keeps its data only in memory and as we are building a durable database, we had to add a persistence layer that stores and retrieves data from the filesystem efficiently.

A Hitchhiker tree is a functional data structure invented by [David Greenberg](https://github.com/dgrnbrg) that incorporates a fractal tree which leads to very efficient read and write operations. One may imagine them as a combination of a [B+-Tree](https://en.wikipedia.org/wiki/B%2B_tree) and an append-only log. There is a wonderful presentation about the hitchhiker tree from [strangeloop 2016](https://www.youtube.com/watch?v=jdn617M3-P4) which I can recommend watching.

<!-- With datascript we have access to a powerful query engine that implements most of the datomic API. An in-depth introduction to datalog-querying and datahike is planned for another blog post and screen cast. -->

The [dat project](https://datproject.org/) is a nonprofit data sharing peer-to-peer protocol for building distributed applications. Currently dat is primarily implemented in JavaScript with some API for high and low-level data synchronization. In the context of this example we will use the command line tools where we only have one-way data replication with one master data silo and many slave data silos. Although the dat project has also some prototypes for [multi-writer support](https://blog.datproject.org/2018/05/14/dat-shopping-list) we at [lambdaforge](http://lambdaforge.io) plan to implement a two way replication for our own index data structure but it takes a little bit more finesse handling the conflict resolution on network faults.

# Demo application

The following demo will show an example application with two peers and a simple database containing users with name and age. First we will create a master database with some entries, create a dat repository in the database folder and then share it to another folder on the system by cloning and synchronizing it with the dat cli client. Then we will attempt to read the cloned database again and try having live updates through file watchers. The following code snippets require basic programming knowledge as well as some understanding of [Lisp](https://en.wikipedia.org/wiki/Lisp_(programming_language)) and command line interactions. 

## Setup
First make sure you have dat installed. See the official [docs](https://docs.datproject.org/install) for further instructions. Also as we are using [Clojure](https://clojure.org/), you need the JVM (check your operating system for Java options) and [leiningen](https://leiningen.org/) in order to execute the code. If you don't want to type you may find the example code [here](https://github.com/kordano/datahike-sync).  This example aims for Unix environments like Linux or macOS but it should easily be translatable to Windows environments (just contact me if you need help with that). 


## Clojure master peer
Let's start our project from scratch. Open a terminal and begin with:

```bash
lein new datahike-replication
cd datahike-replication
```

Add the following to the `:dependencies` section in your `project.clj`.

```clojure
[io.replikativ/datahike "0.1.2"]
[juxt/dirwatch "0.2.3"]
```

Now we can fire up a repl and start tinkering.

```clojure
lein repl
=>
nREPL server started on port 49731 on host 127.0.0.1 - nrepl://127.0.0.1:49731
REPL-y 0.3.7, nREPL 0.2.13
Clojure 1.9.0
Java HotSpot(TM) 64-Bit Server VM 1.8.0_144-b01
    Docs: (doc function-name-here)
          (find-doc "part-of-name-here")
  Source: (source function-name-here)
 Javadoc: (javadoc java-object-or-class-here)
    Exit: Control+D or (exit) or (quit)
 Results: Stored in vars *1, *2, *3, an exception in *e

user=>
```

First we initialize an empty datahike instance within our local folder `/tmp/master-dat`.

```clojure
(require '[datahike.api :as d])

(def master-dir "/tmp/master-dat")
(def master-uri (str "datahike:file://" master-dir))

(d/create-database master-uri)

(def master-conn (d/connect master-uri))
```

Now we add some initial data.


```clojure
@(d/transact master-conn 
             [{:db/id 1 :name "Alice" :age 33}
              {:db/id 2 :name "Bob" :age 37}
              {:db/id 3 :name "Charlie" :age 55}])
```

Let's check if everything is transacted correctly.

```clojure
(d/q '[:find ?n
       :where [?e :name ?n]]
     @master-conn)

;; => #{["Charlie"] ["Alice"] ["Bob"]}
```

Because we are using this query several times with different connections let's make it into a function.

```clojure
(defn get-all-names [conn]
  (d/q '[:find ?n
         :where [?e :name ?n]]
       @conn))
```

## dat replication
Now we initialize the dat master peer in the datahike folder and start sharing it.

```bash
cd /tmp/master-dat
dat create
dat share
```

The share output should give you the dat ID. Something like:

```bash
dat v13.10.0
dat://1058cd5f2d32fb9727bd7b9b6aa97d96a97d42c35173e566df993352db3c8b66
```

Now we can share this link and start distributing the database. Let's do that locally by first cloning the repository into `/tmp/slave-dat`.

```bash
cd /tmp
dat clone dat://1058cd5f2d32fb9727bd7b9b6aa97d96a97d42c35173e566df993352db3c8b66 slave-dat
```

With the cloning done, we can start synchronizing continuously.

```bash
cd /tmp/slave-dat
dat sync
```

Now everything is set up and we can check out the replicated datahike instance.

## Clojure slave peer
Let's go back into our open repl:

```clojure
(def slave-dir "/tmp/slave-dat")
(def slave-uri (str "datahike:file://" slave-dir))
(def slave-conn (d/connect slave-uri))

(get-all-names slave-conn)

;; => #{["Charlie"] ["Alice"] ["Bob"]}
```

We have cloned the data once but updates through the synchronization are not realized in the datahike connection. Let's check this out by adding something to the master database.

```clojure
@(d/transact master-conn [{:db/id 4 :name "Dorothy"}])

(get-all-names slave-conn)
;; => #{["Charlie"] ["Dorothy"] ["Alice"] ["Bob"]}
```

If you look at the terminal process where the slave synchronizing is happening you may see some updates coming through. Now let's have a look at the slave-connection:

```clojure
(get-all-names slave-conn)
;; => #{["Charlie"] ["Alice"] ["Bob"]}
```

Tough luck. Nothing happened. The datahike in-memory index is not in sync. We need to re-establish the local datahike connection in order to get the updates in memory.

```clojure
(def slave-conn (d/connect slave-uri))

(get-all-names slave-conn)
;; => #{["Charlie"] ["Dorothy"] ["Alice"] ["Bob"]}
```

Alright, now we have something. Next, let's update the connection whenever something in the slave peer has changed. Therefore we add a file watcher to the meta directory in our datahike local directory. Let's start with adding a simple watcher:

```clojure
(require '[juxt.dirwatch :refer [watch-dir close-watcher]])

(def slave-state (atom {:conn (d/connect slave-uri) :watcher nil}))
(def slave-meta-dir (clojure.java.io/file (str slave-dir "/meta")))

(defn on-meta-change [state file]
  (prn "File changed:" (.getPath (:file file)))
  (swap! state assoc :conn (d/connect slave-uri)))

(swap! slave-state assoc :watcher (watch-dir
                                   (partial on-meta-change slave-state)
                                   slave-meta-dir))
```

The updates should now be in the database whenever we transact something in the master connection wait a couple seconds and then check the slave connection.

```clojure
@(d/transact master-conn [{:db/id 5 :name "Eve"}])

(get-all-names master-conn)
;; => #{["Charlie"] ["Dorothy"] ["Alice"] ["Eve"] ["Bob"]}
```
Wait a second or two for dat to synchronize the data. Then we have to check the connection in the slave state, as it is reset every time dat synchronizes new data.

```clojure
(get-all-names (:conn @slave-state))
;; => #{["Charlie"] ["Dorothy"] ["Alice"] ["Eve"] ["Bob"]}
```
Awesome, now we have distributed database.

# Live System

For your convenience I've deployed a test dat repository with the above data on one of our servers that you can synchronize against. Make sure you have dat and leiningen installed. I've added all necessary scripts in the [example repository](https://github.com/kordano/datahike-sync). Just clone the repo, run the `clone-test-repo.sh` script and start a repl:

```bash
git clone https://github.com/kordano/datahike-sync.git /tmp/datahike-sync
cd /tmp/datahike-sync
sh ./clone-test-repo.sh
lein repl
```

Now we can create a connection to the cloned data repository:

```clojure
(require '[datahike.api :as d])
(def fast-uri "datahike:file:///tmp/datahike-sync/fast-dat")
(def fast-conn (d/connect fast-uri))
(d/q '[:find ?n
         :where [?e :name ?n]]
       @fast-conn)
       
;; => #{["Charlie"] ["Dorothy"] ["Alice"] ["Eve"] ["Bob"]}
```

The data is correct, we have successfully replicated the demo database.


# Conclusion and future development
All in all we have a pretty quick solution for simple data replication without typing any synchronization code. Two nice things can be accomplished this way: first we can easily scale horizontally be just adding more peers, and secondly we have a painless backup solution without configuring anything. Bear in mind we only have a one-way synchronization, so updates from any other peer than the master peer are not taken into account.

As for datahike itself we at [lambdaforge](http://lambdaforge.io) are planning to integrate efficient index-replication based on dat. Additionally we want to build simple APIs both on the JVM and in the JavaScript environment. Also check here for my blog post about introduction to triplestores with datalog and datahike in a couple of weeks.
