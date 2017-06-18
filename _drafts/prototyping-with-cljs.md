---
layout: post
title: Rapid Web App Prototyping with Clojurescript
introduction: A short trip through web development via Om, a beautiful React-wrapper in Clojurescript, and replikativ, a simple synchronisation infrastructure
---
Before the rise of single page and mobile applications all of the state was handled on the server side by interacting directly with the databases. For the single page applications we need another layer of abstraction to coordinate the state, maybe some REST API that handles all the relevant state changes between clients and databases. That pattern complected the development process in many ways and this makes it rather difficult to reason about a product.   

In this short guide I will show you how to build simple prototypes without the hassle of complicated server development. As view representation we will use a React-wrapper in Clojurescript and as state management we will use [replikativ](http://replikativ.io), a synchronisation infrastructure, that a good [friend](https://whilo.github.io/) and I built over the last years, written in both Clojure and Clojurescript. I'll write about replikativ in general in another blog post because in this post I want to focus on web app prototyping.   

So, first the [prototype](#the-prototype) is described, then we will [setup](#the-setup) our environment, develop our minimalistic [backend](#the-backend) and finally build our [frontend](#the-frontend).


# <a name="the-prototype"></a> The Prototype

As a freelancing software developer, tracking time of your tasks is inevitable when you don't want to lose yourself. So, let's build something that captures the time we spend on different projects and tasks. What should it look like, you say? Well, for the prototype, let's have only small views because we want to build our app as fast and as simple as possible. Basically we want some input that capture our working time of a task at a project and we want to view the list of captured entries. Later on we might add some more features like search, sorting or project popups, but for the moment these two features, an input and a list, should suffice.  

Now let's start with the setup.


#  <a name="the-setup"></a> The Setup

In order to follow the steps you should be a little bit familiar with web development and Clojure. First make sure, you have everything ready for some Clojure development, mainly a recent Java version, [leiningen](https://leiningen.org) and a your prefered editor. For Clojure I'm using [emacs](https://www.gnu.org/software/emacs/) because of the nice integrations that [cider](https://github.com/clojure-emacs/cider) provides.   

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

These libraries are:

- [replikativ](http://replikativ.io/) handles all our state synchronisation between a server peer and clients
- [om](https://github.com/omcljs/om/) provides the representation layer on the client
- [sablono](https://github.com/r0man/sablono) makes templating easy as we can use simple Clojure data structures for html elements
- [http-kit](http://www.http-kit.org/) serves all the assets and base index html 
- [compojure](https://github.com/weavejester/compojure) handles routing for us   

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

# <a name="the-backend"></a> The Backend

Alright, now can start writing the server. Let's open `src/clj/stechuhr/core.clj` and start a repl-session. First we should add all our dependencies, so the head of the file might look something like:

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

# <a name="the-frontend"></a> The Frontend

(Some intro to client dev ...)

## Synchronisation Setup

Now we can start with the web client. Let's start a figwheel repl:

```
lein figwheel
```

Wait a little bit and then we can start editing `src/cljs/stechuhr/core.cljs`. Again we need to add all the dependencies:

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

First we define some constants and our state atom where we all our captures will be stored:
```
(def user "mail:alice@stechuhr.de")
(def ormap-id #uuid "07f6aae2-2b46-4e44-bfd8-058d13977a8a")
(def uri "ws://127.0.0.1:31778")
(defonce val-atom (atom {:captures #{}}))
```

Next we need to setup replikativ with some streaming functions and initialization of store, peer and stage as well as connection to the server peer.   

The streaming evaluation functions need to be defined as a hashmap:

```
(def stream-eval-fns
  {'add (fn [a new]
            (swap! a update-in [:captures] conj new)
            a)
   'remove (fn [a new]
             (swap! a update-in [:captures] (fn [old] (set (remove #{new} old))))
             a)})
```

We have here an add function that appends a new capture to the existing list, and a remove function that retracts a given capture from our capture list. Like in the backend we also write a setup function for replikativ:

```
(defn setup-replikativ []
  (go-try
   S
   (let [store  (<? S (new-mem-store)) ; (1)
         peer   (<? S (client-peer S store)) ; (2)
         stage  (<? S (create-stage! user peer)) ; (3)
         stream (stream-into-identity! stage [user ormap-id] stream-eval-fns val-atom)] ; (4)
     (<? S (s/create-ormap! stage :description "captures" :id ormap-id)) ; (5)
     (connect! stage uri) ; (6)
     {:store  store
      :stage  stage
      :stream stream
      :peer   peer}))) ; (7)
```

This looks familiar but let's go through it step by step:

1. First we initialize a key-value store
2. Then we start a peer 
3. We initialize a stage which is the replikativ state we are interacting with
4. Then we initialize the streaming interface that applies all changes to our local state
5. Now we create the replicated data type we are working with, in this case an OR-Map
6. Next we connect our stage with the server peer
7. Finally we return all the different values in a hashmap

For simplicity we wrap the only stage interaction in a function

```
(defn add-capture! [state capture]
  (s/assoc! (:stage state)
            [user ormap-id]
            (uuid capture)
            [['add capture]]))
```

By choosing the hash of the data as key we convert the OR-Map into an OR-Set. Now we are good to go on the state and data side, let's move on to the UI side.    

## User Interface

First we create the base component with a plain `div`:

```
(defui App
  Object
  (render [this]
    (html [:div [:h1 "Hello Stechuhr]"])))
```

Then we set it as root:

```
(def reconciler
  (om/reconciler {:state val-atom}))

(om/add-root! reconciler App (.getElementById js/document "app"))
```

Let's see what this looks like: open the browser at [http://localhost:3449](http://localhost:3449) where you should see **Hello Stechuhr**. Awesome, now we can rapidly extend the page. Maybe we start with the input field for our captures:

```
(defn input-widget [component placeholder local-key]
  [:input {:value (get (om/get-state component) local-key)
           :placeholder placeholder
           :on-change (fn [e]
                        (om/update-state!
                         component
                         assoc
                         local-key
                         (.. e -target -value)))}])
```

Before we can use this widget we should add local state for the three input elements:

```
(defui App
  Object
  (componentWillMount                         ;  ⇐
    [this]                                    ;  ⇐
    (om/set-state! this {:input-project ""    ;  ⇐
                         :input-task    ""    ;  ⇐
                         :input-capture ""})) ;  ⇐ 
  (render [this]
    (html [:div [:h1 "Hello Stechuhr"]])))
```

Now we can add all the input widgets:
 
```
(defui App
  Object
  (componentWillMount
    [this]
    (om/set-state! this {:input-project ""
                         :input-task    ""
                         :input-capture ""}))
  (render [this]
    (html
     [:div
      [:div.widget                                       ;  ⇐
       [:h1 "Task Captures"]                             ;  ⇐
       (input-widget this "Project" :input-project)      ;  ⇐
       (input-widget this "Task" :input-task)            ;  ⇐
       (input-widget this "Capture" :input-capture)]]))) ;  ⇐
```

We should see a headline and three input elements by now on the page. This input should be added to our local replikativ state, so we add a button that performs this actions.

```
(defui App
  Object
  (componentWillMount
   [this]
   (om/set-state! this {:input-project ""
                        :input-task ""
                        :input-capture ""}))
  (render [this]
    (let [{:keys [input-project                                     ;  ⇐
                  input-task                                        ;  ⇐
                  input-capture]} (om/get-state this)               ;  ⇐
          {:keys [captures]} (om/props this)]                       ;  ⇐
      (html
       [:div
        [:div.widget
         [:h1 "Task Captures"]
         (input-widget this "Project" :input-project)
         (input-widget this "Task" :input-task)
         (input-widget this "Capture" :input-capture)
         [:button                                                   ;  ⇐
          {:on-click                                                ;  ⇐
          (fn [_]                                                   ;  ⇐
            (let [new-capture {:project input-project               ;  ⇐
                               :task input-task                     ;  ⇐
                               :capture input-capture}]             ;  ⇐
              (do                                                   ;  ⇐
                (add-capture! replikativ-state new-capture)         ;  ⇐
                (om/update-state! this assoc :input-project "")     ;  ⇐
                (om/update-state! this assoc :input-task "")        ;  ⇐
                (om/update-state! this assoc :input-capture ""))))} ;  ⇐
          "Add"]]]))))                                              ;  ⇐
```

The button's `on-click` callback now retrieves the input data, builds our capture model, adds the data to our replikativ state and cleans up the local state. If we now press the button, open the developer javascript console, we can see that some things happen in replikativ. That shouldn't discourage you, it's only logs so far and we are not yet connected to any server systems.   

For that we go back to `src/clj/core.clj` and evaluate the `-main` function. Now we have a running server peer that clients can use.   


But can we connect to this peer? Easy! Let's go back to `src/cljs/stechuhr/core.cljs` and add some main function, that initilizes replikativ with the relevant peer, stage and store. Just add the following above the `reconciler`:

```
(defn main [& args]
  (go-try S (def replikativ-state (<? S (setup-replikativ))))
  (.error js/console "Stechuhr connected ...."))
```
 We have created all necessary replikativ setup functions before, so this is piece of cake!   
 
Notice how the client connects to the server and constantly streams update to the client.

Finally we should add some table that shows our captures:

```
(defui App
  Object
  (componentWillMount
   [this]
   (om/set-state! this {:input-project ""
                        :input-task ""
                        :input-capture ""}))
  (render [this]
    (let [{:keys [input-project input-task input-capture]} (om/get-state this)
          {:keys [captures]} (om/props this)]
      (html
       [:div
        [:div.widget
         [:h1 "Task Captures"]
         (input-widget this "Project" :input-project)
         (input-widget this "Task" :input-task)
         (input-widget this "Capture" :input-capture)
         [:button
          {:on-click
          (fn [_]
            (let [new-capture {:project input-project
                               :task input-task
                               :capture input-capture}]
              (do
                (add-capture! replikativ-state new-capture)
                (om/update-state! this assoc :input-project "")
                (om/update-state! this assoc :input-task "")
                (om/update-state! this assoc :input-capture ""))))}
          "Add"]]
        [:div.widget                            ;  ⇐
         [:table                                ;  ⇐
          [:tr                                  ;  ⇐
           [:th "Project"]                      ;  ⇐
           [:th "Task"]                         ;  ⇐
           [:th "Capture"]]                     ;  ⇐
          (mapv                                 ;  ⇐
           (fn [{:keys [project task capture]}] ;  ⇐
             [:tr                               ;  ⇐
              [:td project]                     ;  ⇐
              [:td task]                        ;  ⇐
              [:td capture]])                   ;  ⇐
           captures)]]]))))                     ;  ⇐
```

Now we should see the captures that we added before. Now let's check if this data synchronises to other browser clients. Therefore we open an new browser window at [http://localhost:3449](http://localhost:3449) where we can observe the newly added captures.

And that's it, we have a small prototype with just some lines of code with a fully functional backend, that replicates all data to clients, provides realtime updates and supports offline capabilities.

(Maybe some final words and replikativ future ...)
