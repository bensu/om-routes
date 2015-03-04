# om-routes

[![Clojars Project](http://clojars.org/om-syncing/latest-version.svg)](http://clojars.org/om-syncing)

Users expect to use the browser's navigation tools
to work even inside SPAs. This library binds the browser's
url to some cursor in your app-state so that you can model your
navigation however you like and it will automatically make it work
with the browser's navigation tools.

It has the same structure than
[om-sync](http://github.com/swannodette/om-sync). Instead of syncing a cursor's
state with an external source, it syncs it with the navbar's url.

## Example

Should cook up a better example. It should:

1. Show how nav state functions as normal state for rest of components.
2. Show how other state is orthogonal and not remembered.
3. Show how both links and the navbar can be used to affect the state.
4. Show that it is up to the developer to choose which states are
   reachable from links/navbar or from direct manipulation via other events.


## Contributions

Pull requests, issues, and feedback welcome.

## License

Copyright © 2015 Sebastian Bensusan

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
