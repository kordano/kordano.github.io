---
layout: scientific_post
title: Datahike Prototype Performance
introduction: Query performance Comparison among transactional databases in Clojure
script: /js/datahike_benchmark_plots.js
---

In the following article I will establish a quick performance comparison among our own database datahike and comparable transactional databases in the Clojure ecosystem.

#  The lineup
While it may be useful to compare datahike to traditional RDMS I will focus only on databases which use comparable query engines and which would be considered equivalent systems. In the Clojure world datomic and datascript are the established databases using datalog.
<!-- overall reason why both are special-->

## Datomic

## Datascript

## Datahike


# Methodology
The focus is set on query performance on im-memory indexed and non-indexed data with different sizes. Here I started with 10 thousand, 100 thousand and 1 million entries of simple data with following model:

```clojure
{:name "Alice"
 :age 42}
``` 
Within the database `:name` will be introduced as an indexed attribute.

The data is queried using [datalog](https://en.wikipedia.org/wiki/Datalog) with increasing complexity focussing on indexes and plain non-index data. 

As benchmarking toolkit I chose [critierium](https://github.com/hugoduncan/criterium) for its ease of use and extensive testing and reporting capabilities as shown below.

```Shell
Warming up for JIT optimisations 10000000000 ...
  compilation occurred before 1 iterations
  compilation occurred before 74 iterations
  compilation occurred before 147 iterations
  compilation occurred before 220 iterations
  compilation occurred before 293 iterations
  compilation occurred before 366 iterations
  compilation occurred before 439 iterations
  compilation occurred before 512 iterations
  compilation occurred before 585 iterations
  compilation occurred before 877 iterations
Estimating execution count ...
Sampling ...
Final GC...
Checking GC...
Finding outliers ...
Bootstrapping ...
Checking outlier significance
x86_64 Mac OS X 10.12.6 4 cpu(s)
Java HotSpot(TM) 64-Bit Server VM 25.144-b01
Runtime arguments: -Dfile.encoding=UTF-8 -XX:-OmitStackTraceInFastThrow -Dclojure.compile.path=/Users/konny/customers/lambdaforge/datahike-benchmark/target/classes -Ddatahike-benchmark.version=0.1.0-SNAPSHOT -Dclojure.debug=false -javaagent:/Applications/IntelliJ IDEA CE.app/Contents/lib/idea_rt.jar=50787:/Applications/IntelliJ IDEA CE.app/Contents/bin
Evaluation count : 4500 in 60 samples of 75 calls.
      Execution time sample mean : 14.423571 ms
             Execution time mean : 14.431296 ms
Execution time sample std-deviation : 1.486105 ms
    Execution time std-deviation : 1.541885 ms
   Execution time lower quantile : 13.454645 ms ( 2.5%)
   Execution time upper quantile : 17.449154 ms (97.5%)
                   Overhead used : 2.144902 ns

Found 10 outliers in 60 samples (16.6667 %)
	low-severe	 5 (8.3333 %)
	low-mild	 5 (8.3333 %)
 Variance from outliers : 72.1065 % Variance is severely inflated by outliers
``` 

<!-- use big machine for benchmarking -->

The full source code for the benchmarks can be found [here](https://github.com/kordano/datahike-benchmark).

# Results


## Transaction
As first benchmark the execution time of data insertion is observed. First 10 thousand, then 100 thousand and finally 1 million entries are inserted.

<canvas id="insertionChart"></canvas>

## Query
For a baseline one of the fastest queries is pulling an entry as index.
```Clojure
[:find ?e :where [?e :name "user99"]]
```

<canvas id="queryChart1"></canvas>
<!-- Insert  -->
