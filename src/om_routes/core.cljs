(ns om-routes.core 
    (:require-macros [cljs.core.async.macros :refer [go]])
    (:require [om.core :as om]
              [cljs.core.async :as async :refer [put! chan]]
              [bidi.bidi :as bidi]
              [goog.events :as events]
              [goog.history.EventType :as EventType])
    (:import goog.History))

(defn- go-to
  "Goes to the specified url"
  [url]
  (.assign (.-location js/window) url))

(defn- to-indexed
  "Makes sure the cursor-path is a []"
  [cursor-path] 
  {:pre [(or (vector? cursor-path) true)]}  ;; FIX: add (atomic?)
  (if (vector? cursor-path)
    cursor-path
    [cursor-path]))

(defn make-handler
  "Takes a handler and adds a bidi tag to it"
  [handler]
  (bidi/->IdentifiableHandler ::handler handler))

(def debug-on? (atom false))

(defn print-log [& args]
  (if @debug-on? 
    (apply println args)))

(def valid-opts #{:view-component :route :debug :nav-path :opts :history-target})

(defn om-routes
  "Creates a component that tracks a part of the state and syncs it
  with the navbar to support the back, forward, and refres buttons.
  In order to function you must provide a subscribeable core.async
  channel that  will stream all :tx-listen events. This channel must be
  called :tx-chan and provided via the :share option to om.core/root.
  It must be a channel constructed with cljs.core.async/pub with the
  topic :txs.
  Once built om-routes will act on any transactions to the nav-value
  regardless of depth. In order to identify which transactions to act
  on these transactions must be labeled with :om-routes.core/nav 
  om-routes takes a variety of options via the :opts passed to
  om.core/build:
  :view-component - a required Om component function to render the app.
  :nav-path - the path into the app-state that should be tracked for navigation 
  :routes - a Bidi handler that matches the url to a state map
  -- Optional
  :debug - defaults to false, turns on verbose output for debugging
  :history-target - InputTagElement used by goog.History
  :opts - regular om options map to pass along to the view-component"
  [data owner opts]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [nav-path (to-indexed (:nav-path opts))
            route (:route opts)
            tx-chan (om/get-shared owner :tx-chan)
            txs (chan)]
        (if (:debug opts) (reset! debug-on? true))
        (async/sub tx-chan :txs txs)
        (om/set-state! owner :txs txs)
        (go (loop []
              (let [[{:keys [new-state tag]} _] (<! txs)]
                (print-log "Got tag:" tag)
                (when (= ::nav tag)
                  (let [params (get-in new-state nav-path)
                        _ (print-log "Got state:" params)
                        url (apply bidi/path-for route ::handler
                                   (reduce concat (seq params)))]
                    (print-log "with url:" url)
                    (go-to url)))
                (recur))))
        (doto (if-let [ht (:history-target opts)] 
                (History. false nil (:history-target opts))
                (History.))
          (goog.events/listen 
           EventType/NAVIGATE
           (fn [url]
             (print-log "Got url:" (.-token url))
             (let [{:keys [handler route-params]}
                   (bidi/match-route route (str "#" (.-token url)))]
               (print-log "that matched" (if (nil? handler) 
                                           "no handlers"
                                           "a handler"))
               (print-log "with params:" route-params)
               (if-not (nil? handler)
                 (om/update! data nav-path (handler route-params))))))
          (.setEnabled true))))
    om/IRender
    (render [_]
      (om/build (:view-component opts) data {:opts (:opts opts)}))))
