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

Create an `app-state` with a navigation component:


## Contributions

Pull requests, issues, and feedback welcome.

## License

Copyright © 2015 Sebastian Bensusan

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
