# om-routes

[![Clojars Project](http://clojars.org/om-syncing/latest-version.svg)](http://clojars.org/om-syncing)

Users expect to use the browser's navigation tools
to work even inside SPAs. This library attempts to bind the browser's
url to some cursor in your app-state so that you can model your
navigation however you like and it will automatically make it work
with the browser's navigation tools.

There are advantages to model our app's navigation properties in the
main state we are using for Om. First: it is a Clojure data structure
allowing us to modify it, copy it, validate it using Clojure. Second
if it is part of the main cursor, the app will appropriately reload
when it is changed.

It is based on [om-sync](http://github.com/swannodette/om-sync) and it
follows the same structure: sync a cursor's state with an external
source, but in this case it is browser's navbar instead of the server.

## Example

TODO

## Contributions

Pull requests, issues, and feedback welcome.

## License

Copyright © 2015 Sebastian Bensusan

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
