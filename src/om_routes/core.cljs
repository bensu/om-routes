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

(defn om-routes
  "FIX"
  [data owner opts]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [nav-path (to-indexed (:nav-path opts))
            route (:route opts)
            tx-chan (om/get-shared owner :tx-chan)
            txs (chan)]
        (async/sub tx-chan :txs txs)
        (om/set-state! owner :txs txs)
        (go (loop []
              (let [[{:keys [new-state tag]} _] (<! txs)]
                (when (= ::nav tag)
                  (let [params (get-in new-state nav-path)
                        url (apply bidi/path-for route ::handler
                                   (reduce concat (seq params)))]
                    (go-to url)))
                (recur))))
        (let [h (History.)]
          (goog.events/listen
           h EventType/NAVIGATE
           (fn [url]
             (let [{:keys [handler route-params]}
                   (bidi/match-route route (str "#" (.-token url)))]
               (if-not (nil? handler)
                 (om/update! data nav-path (handler route-params))))))
          (doto h (.setEnabled true)))))
    om/IRender
    (render [_]
      (om/build (:view-component opts) data))))
