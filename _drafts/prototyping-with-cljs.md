---
layout: post
title: Rapid Web App Prototyping with Clojurescript
introduction: A short trip through web development via Om, a beautiful React-wrapper in Clojurescript, and replikativ, a simple synchronisation infrastructure
---
Before the rise of single page and mobile applications all of the state was handled on the server side by interacting directly with the databases. For the single page applications we need another layer of abstraction to coordinate the state, maybe some REST API that handles all the relevant state changes between clients and databases. That pattern complected the development process in many ways and it makes it rather difficult to extend and manage a product.   

In this short guide I will show you how to build simple prototypes without the hassle of complicated server development. In order to follow the steps you should be a little bit familiar with web development and Clojure.   

First make sure, you have everyting ready for some Clojure development, mainly a recent Java version, [leiningen](https://leiningen.org) and a your prefered editor. For Clojure I'm using emacs because of the nice integrations that [cider](https://github.com/clojure-emacs/cider) provides.   

Alright let's start by creating a new figwheel project:

```shell
lein new figwheel stechuhr
cd stechuhr
```

Let's add the relevant libraries to the `:dependencies` in our `project.clj`:

```clojure
[io.replikativ/replikativ "0.2.1"]
[org.omcljs/om "1.0.0-alpha46"]
[sablono "0.7.6"]
[http-kit "2.2.0"]
[compojure "1.5.2"]
```

[replikativ](http://replikativ.io/) handles all our state synchronisation between a server peer and clients, [om](https://github.com/omcljs/om/) is be our representation layer on the client, [sablono](https://github.com/r0man/sablono) makes templating easy as we can use simple Clojure data structures for html elements, [http-kit](http://www.http-kit.org/) serves all the assets and base index html and [compojure](https://github.com/weavejester/compojure) handles routing for us.   

Now we create an `index.html` in `resources/public` as our root web app container with the following content:   

```html
<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link rel="stylesheet" type="text/css" href="./css/style.css">
    <title>Stechuhr: Time Capturing</title>
  </head>
  <body class="h-100 w-100">
    <div id="app"></div>
    <script src="js/compiled/stechuhr.js" type="text/javascript"></script>
    <script type="text/javascript">
        stechuhr.core.main();
    </script>
  </body>
</html>
```   

For the server we have to adjust the project structure a little bit:   

```shell
mv src/* src/cljs
mkdir -p src/clj/stechuhr
touch src/clj/stechuhr/core.clj
```   

These changes should also be reflected as `:source-paths` in our `project.clj`:   

```clojure
["src/cljs" "src/clj"]
``` 
     
The final project structure should look like this:

```bash
├── CHANGELOG.md
├── LICENSE
├── README.md
├── dev
├── doc
│   └── intro.md
├── project.clj
├── resources
│   └── public
│       └── index.html
├── src
│   ├── clj
│   │   └── stechuhr
│   │       └── core.clj
│   └── cljs
│       └── stechuhr
│           └── core.cljs
└── test
    └── stechuhr
        └── core_test.clj
```

Alright, now can start writing the server. Let's open `src/clj/core.clj` and start a repl-session. First we should add all our dependencies, so the head of the file might look something like:

```clojure
(ns stechuhr.core
  (:require [hasch.core :refer [uuid]]
            [replikativ.peer :refer [server-peer]]
            [kabel.peer :refer [start stop]]
            [konserve.memory :refer [new-mem-store]]
            [superv.async :refer [<?? S]]
            [clojure.core.async :refer [chan] :as async]
            [org.httpkit.server :refer [run-server]]
            [compojure.route :refer [resources not-found]]
            [compojure.core :refer [defroutes]]))
```

Next we should define the base routes for the HTTP server:

```clojure
(defroutes base-routes
  (resources "/")
  (not-found "<h1>404. Page not found.</h1>"))
```

Now we write a function that starts all our services:

```clojure
(defn start-server []
  (let [uri   "ws://127.0.0.1:31778"
        store (<?? S (new-mem-store)) ; (1)
        peer  (<?? S (server-peer S store uri))] ; (2)
    (run-server #'base-routes {:port 8080}) ; (3)
    (<?? S (start peer)) ; (4)
    (<?? S (chan)))) ; (5)
```

So, what's happening here?     
 
1. First we create an in-memory store that holds all our data
2. Then we create a replikativ peer that writes its data to the store and provides a socket at the given uri.
3. Now we start a http-kit server at port 8080 that provides the html and all our assets
4. Next we start the replikativ peer 
5. Finally we block the thread for ongoing execution.

Now we only need to add a main routine to start our system:

```clojure
(defn -main [& args]
  (start-server))
```

That's it! We don't need anything more for the backend. Pretty nice!   


Now we can start with the web client. Let's open `src/cljs/core.cljs` and add again all the dependencies we need:

```clojure
(ns stechuhr.core
  (:require [konserve.memory :refer [new-mem-store]]
            [replikativ.peer :refer [client-peer]]
            [replikativ.stage :refer [create-stage! connect! subscribe-crdts!]]
            [hasch.core :refer [uuid]]
            [replikativ.crdt.ormap.realize :refer [stream-into-identity!]]
            [replikativ.crdt.ormap.stage :as s]
            [cljs.core.async :refer [>! chan timeout]]
            [superv.async :refer [S] :as sasync]
            [om.next :as om :refer-macros [defui] :include-macros true]
            [om.dom :as dom :include-macros true]
            [sablono.core :as html :refer-macros [html]])
  (:require-macros [superv.async :refer [go-try <? go-loop-try]]
                   [cljs.core.async.macros :refer [go-loop]]))
```
