# om-routes

[![Clojars Project](http://clojars.org/om-routes/latest-version.svg)](http://clojars.org/om-routes)

Users expect to use the browser's navigation tools
to work even inside SPAs. This library binds the browser's
url to some cursor in your app-state so that you can model your
navigation however you like and it will automatically make it work
with the browser's navigation tools.

It has the same structure than
[om-sync](http://github.com/swannodette/om-sync). Instead of syncing a cursor's
state with an external source, it syncs it with the navbar's url.

## Minimal Example

We are going to code a simple widget that tracks one piece of state:
if the last click was done on a button or on a link. First we start by
setting the state:

```clj
(defonce app-state (atom {:nav {:method :state}}))
```

We are using `defonce` in case we are live code reloading ([Figwheel](https://github.com/bhauman/lein-figwheel))

Now we define how that state should be accessed and modified:

```clj
(def nav-path :nav)

(defn get-nav [data]
  (get-in data [nav-path :method]))

(defn nav-to [view-cursor method]
  (om/update! view-cursor [nav-path :method] method :routing.core/nav))
```

Note that we are tagging every `update!` to the navigation state with a namespace
qualified keyword. Now we define a one-to-one mapping between the
navigation state and a url, producing a [Bidi] handler, `route`:

```clj
(defn url->state [{:keys [method]}]
  {:method (keyword method)})

(def route [["#" :method] (routing/make-handler url->state)])
```

Note that the `route` matches links that begin with `#` since we want
to specify
[Fragements](http://en.wikipedia.org/wiki/Fragment_identifier) of our
website and not other external resources. All routes should start with `#`.

Now let's implement the view:

```clj
(defn view-component [data owner]
  (om/component
   (dom/div nil
            (dom/h1 nil (case (get-nav data)
                          :state "A button got me here"
                          :link "A link got me here"
                          "Who got me here?"))
            (dom/button #js {:onClick (fn [_] (nav-to data :state))} "Button") 
            (dom/br nil)
            (dom/a #js {:href "#link"} "Link"))))
```

As you can see, the link also uses `#` for its `href` property.

Then we set up a pub-channel for all the transactions. `om-routes`
will listen to those with the `:routing.core/nav` tag. Finally, we
wrap the view with `om-routes` by passing `view-component`, `route`,
and `nav-path` as `opts`:

```clj
(let [tx-chan (chan)
      tx-pub-chan
      (async/pub tx-chan (fn [_] :txs))]
  (om/root
   (fn [data owner]
     (reify
       om/IRender
       (render [_]
         (om/build routing/om-routes data
                   {:opts {:view-component view-component
                           :route route
                           :nav-path nav-path}}))))
   app-state
   {:target (. js/document (getElementById "app"))
    :shared {:tx-chan tx-pub-chan}
    :tx-listen (fn [tx-data root-cursor]
                 (put! tx-chan [tx-data root-cursor]))}))
```

FIX: Add GIF.

## Contributions

Pull requests, issues, and feedback welcome.

## License

Copyright © 2015 Sebastian Bensusan

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
