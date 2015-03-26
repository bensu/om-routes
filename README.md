# om-routes

## Installing

Not Ready for Use

<!--
[![Clojars Project](http://clojars.org/om-routes/latest-version.svg)](http://clojars.org/om-routes)
-->

    (:require [om-routes.core :as routes])

## Description

Users expect to use the browser's navigation tools
to work everywhere, even inside SPAs. This library binds the browser's
url to some cursor in your app-state so that you can model your
navigation however you like and it will automatically make it work
with the browser's navigation tools.

It follows [om-sync](http://github.com/swannodette/om-sync)'s
structure. Instead of syncing a cursor's state with an external
source, it syncs it with the navbar's url.

It amounts to very little code and is probably not *exactly* what you
need, yet I find it a useful pattern and worth considering.

```clj

(defonce app-state (atom {:nav {:last-click nil}}))

(def route [["#" :last-click]
            (routes/make-handler #(update-in % [:last-click] keyword))])

(let [tx-chan (chan)
      tx-pub-chan (async/pub tx-chan (fn [_] :txs))]
  (om/root
   (fn [data owner]
     (reify
       om/IRender
       (render [_]
         (om/build routes/om-routes data
                   {:opts {:view-component view-component
                           :route route
                           :debug true
                           :nav-path nav-path}}))))
   app-state
   {:target (. js/document (getElementById "app"))
    :shared {:tx-chan tx-pub-chan}
    :tx-listen (fn [tx-data root-cursor]
                 (put! tx-chan [tx-data root-cursor]))}))
```

## Minimal Example

We are going to code a simple widget that tracks one thing: if the
last click was made by a button or a link. We can start from a template that
includes Om, core.async, and
[Figwheel](https://github.com/bhauman/lein-figwheel) for easier
development:

    lein new figwheel routes-example -- --om
    cd routes-example

We start by adding `om-routes` to `project.clj`:

```clj
:dependencies [[org.clojure/clojure "1.6.0"]
               [org.clojure/clojurescript "0.0-3126"]
               [figwheel "0.2.5"]
               [org.clojure/core.async "0.1.346.0-17112a-alpha"]
               [sablono "0.3.4"]
               [org.omcljs/om "0.8.8"]
               [om-routes "0.1.1-SNAPSHOT"]] ;; <- Add this
```

Then by editing `src/routes-example/core.cljs` and adding some
requirements:

```clj
(ns routes-example.core
    (:require-macros [cljs.core.async.macros :refer [go]])
    (:require [cljs.core.async :as async :refer [put! chan]]
        	  [om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true]
              [om-routes.core :as routes]))
```

Next we can set the structure of
the state. Everything under `:nav` will be tracked:

```clj
(defonce app-state (atom {:nav {:last-click nil}}))
```

Now we define how the nave state should be accessed and modified:

```clj
(def nav-path :nav)

(defn get-nav [data]
  (get-in data [nav-path :last-click]))

(defn nav-to [view-cursor method]
  (om/update! view-cursor [nav-path :last-click] method :om-routes.core/nav))
```

Note that we are tagging every `update!` to the navigation state with
a namespace qualified keyword, `:om-routes.core/nav`. This definitions
are not strictly necessary for `om-routes` but they are good
programming practice. Now we define a one-to-one mapping
between the navigation state and a url, producing a [Bidi](https://github.com/juxt/bidi) handler,
`route`. This will tell `om-routes` how to transform a url into a
navigation map and backwards:

```clj
(defn url->state [{:keys [last-click]}]
  {:last-click (keyword last-click)})

(def route [["#" :last-click] (routes/make-handler url->state)])
```

Note that `route` matches links that begin with `#` since we want
to specify
[Fragments](http://en.wikipedia.org/wiki/Fragment_identifier) of our
website and not external resources. All routes should start with `#`.

Now let's implement the view:

```clj
(defn view-component [data owner]
  (om/component
   (dom/div nil
            (dom/h1 nil (case (get-nav data)
                          :button "A button got me here"
                          :link "A link got me here"
                          "Who got me here?"))
            (dom/button #js {:onClick (fn [_] (nav-to data :button))} "Button") 
            (dom/br nil)
            (dom/a #js {:href "#link"} "Link"))))
```

As you can see, the link also uses `#` for its `href` property.

Then we set up a pub-channel for all the transactions. `om-routes`
will listen to those tagged with `:om-routes.core/nav`. Finally, we
wrap the `view-component` with `om-routes` by passing it along `route`,
and `nav-path` as `opts`:

```clj
(let [tx-chan (chan)
      tx-pub-chan (async/pub tx-chan (fn [_] :txs))]
  (om/root
   (fn [data owner]
     (reify
       om/IRender
       (render [_]
         (om/build routes/om-routes data
                   {:opts {:view-component view-component
                           :route route
                           :debug true
                           :nav-path nav-path}}))))
   app-state
   {:target (. js/document (getElementById "app"))
    :shared {:tx-chan tx-pub-chan}
    :tx-listen (fn [tx-data root-cursor]
                 (put! tx-chan [tx-data root-cursor]))}))
```

Notice adding the `:debug` option, which normally defaults to
`false`.

FIX: Add GIF.

## Contributions

Pull requests, issues, and feedback welcome.

## License

Copyright © 2015 Sebastian Bensusan

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
