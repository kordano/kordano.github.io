# From simple fork to self-contained project

# abstract
A personal story from the trenches of open source software. Following the idea of an open source durable datalog database in Clojure we forked the well-known in-memory database *datascript* in early 2018 in order to add a durable layer using storage libraries from our *replikativ* projects. With this talk I want to share my experience on how we grew the project while still building an IT consultancy and working client contracts. I will present my personal point of view about our first attempts to publish the fork, the struggle to understand the code base, extending and maintaining the project and the team, as well as decisions that had to be made that lead us away from the original fork, up to the point where we got two projects supporting us: *JobTech Dev* from the swedish public employment service and *datopia*, a distributed public database. After storytime I will show a little bit the database itself, highlight the similarities to *Datomic* and *datascript*, and explain the new features that were developed over the last year as well as future plans.