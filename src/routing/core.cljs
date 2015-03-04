(ns ^:figwheel-always routing.core
    (:require-macros [cljs.core.async.macros :refer [go]])
    (:require [om.core :as om :include-macros true]
              [cljs.core.async :as async :refer [put! chan alts!]]
              [om.dom :as dom :include-macros true]
              [bidi.bidi :as bidi]
              [goog.events :as events]
              [goog.history.EventType :as EventType])
    (:import goog.History))

(enable-console-print!)

(defonce app-state (atom {:view {:mode :edit 
                                 :type 0}
                          :counters [{:count 0}
                                     {:count 0}]}))

(defn view-count [data owner]
  (om/component
   (dom/div nil (:count data))))

(defn edit-count [data owner]
  (om/component
   (dom/button #js {:onClick (fn [_] (om/transact! data :count inc))}
               (:count data))))

(defn nav-to [view-cursor mode]
  (om/update! view-cursor [:view :mode] mode ::nav))

(defn view-component [data owner]
  (om/component
   (dom/div nil
            (apply dom/div nil
                   (case (get-in data [:view :mode])
                     :edit (om/build-all edit-count (:counters data))
                     (om/build-all view-count (:counters data))))
            (dom/h1 nil (name (get-in data [:view :mode])))
            (dom/h1 nil (get-in data [:view :type]))
            ;; The button is the from state to routes binding
            (dom/button #js {:onClick (fn [_] (nav-to data :list))}
                        "List")
            (dom/button #js {:onClick (fn [_] (nav-to data :edit))}
                        "Edit")
            (dom/button #js {:onClick (fn [_] (nav-to data :view))}
                        "View")
            ;; The links are the routes to state binding
            (dom/br nil)
            (dom/a #js {:href "#list/0"} "List")
            (dom/a #js {:href "#edit/0"} "Edit")
            (dom/a #js {:href "#view/0"} "View"))))

;; Things for the API

(def cursor-path :view)

(defn state->url [new-state]
  (str "#" (name (get-in new-state [:mode]))
       "/" (get-in new-state [:type])))

(defn url->state [{:keys [mode type]}]
  {:mode (keyword mode)
   :type (js/parseInt type)})

(def route [[:mode "/" :type] :handler])

(def handler-map {:handler url->state})
;; API

(defn- go-to
  "Goes to the specified url"
  [url]
  (.assign (.-location js/window) url))

;; FIX: Generalize this 
(defn- share-path?
  "Checks if the path has the specified root.
   Ex: root = [:view :mode]
       path = [:view :mode 0 0 1]
       returns true.
   Ex: root = [:view :edit]
       path = [:view :list]
       returns false"
  [root path]
  (= cursor-path (first path)))

(defn- to-indexed
  "Makes sure the cursor-path is a []"
  [cursor-path] 
  {:pre [(or (vector? cursor-path) true)]}  ;; FIX: add (atomic?)
  (if (vector? cursor-path)
    cursor-path
    [cursor-path]))

(defn om-routes [data owner opts]
  (reify
    om/IWillMount
    (will-mount [_]
      (let [cursor-path (to-indexed (:cursor-path opts))]
        (let [tx-chan (om/get-shared owner :tx-chan)
              txs (chan)]
          (async/sub tx-chan :txs txs)
          (om/set-state! owner :txs txs)
          (go (loop []
                (let [[{:keys [new-state tag]} _] (<! txs)]
                  (when (= ::nav tag)
                    (println "state changed")
                    (println new-state)
                    (println (bidi/path-for :handler new-state))
                    #_(go-to ((:state->url opts) (get-in new-state cursor-path))))
                  (recur))))
          (let [h (History.)]
            (goog.events/listen
             h EventType/NAVIGATE
             (fn [url]
               (let [{:keys [handler route-params]}
                     (bidi/match-route route (.-token url))]
                 (println (.-token url))
                 (println route-params)
                 (om/transact! data
                               cursor-path
                               (fn [_] ((handler-map handler) route-params))))))
            (doto h (.setEnabled true))))))
    om/IRender
    (render [_]
      (om/build (:view opts) data))))

;; The only part that is not extracted away is the matching function
;; for the pub channel

(let [tx-chan (chan)
      tx-pub-chan
      (async/pub tx-chan (fn [_] :txs))]
  (om/root
   (fn [data owner]
     (reify
       om/IRender
       (render [_]
         (om/build om-routes data
                   {:opts {:view view-component
                           :cursor-path cursor-path
                           :url->state url->state
                           :state->url state->url}}))))
   app-state
   {:target (. js/document (getElementById "app"))
    :shared {:tx-chan tx-pub-chan}
    :tx-listen (fn [tx-data root-cursor]
                 (put! tx-chan [tx-data root-cursor]))}))


;; Plugin Secretary to Goog History

