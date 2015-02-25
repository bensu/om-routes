(ns ^:figwheel-always routing.core
    (:require-macros [cljs.core.async.macros :refer [go]])
    (:require [om.core :as om :include-macros true]
              [cljs.core.async :as async :refer [put! chan alts!]]
              [om.dom :as dom :include-macros true]
              [secretary.core :as secretary :refer-macros [defroute]]
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

;; Things for the API

(def cursor-path :view)

(def url-pattern "/:mode/:type")

(defn state->url [new-state]
  (str "#" (name (get-in new-state [:mode]))
       "/" (get-in new-state [:type])))

(defn url->state [{:keys [mode type]}]
  {:mode (keyword mode)
   :type (js/parseInt type)})


;; API

(defn- go-to
  "Goes to the specified url"
  [url]
  (.assign (.-location js/window) url))

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

(let [tx-chan (chan)
      tx-pub-chan (async/pub tx-chan
                             (fn [{:keys [path]}]
                               (if (share-path? cursor-path path) :nav)))]
  (om/root
   (fn [data owner]
     (reify
       om/IWillMount
       (will-mount [_]
         (let [cursor-path (to-indexed cursor-path)]
           (defroute om-routes url-pattern {:as params}
             (swap! app-state #(assoc-in % cursor-path (url->state params))))
           (let [tx-chan (om/get-shared owner :tx-chan)
                 txs (chan)]
             (async/sub tx-chan :nav txs)
             (om/set-state! owner :nav txs)
             (go (loop []
                   (let [[v c] (<! txs)
                         {:keys [path new-value new-state]} v]
                     (go-to (state->url (get-in new-state cursor-path))))
                   (recur))))))
       om/IRender
       (render [_]
         (dom/div nil
                  (apply dom/div nil
                         (case (get-in data [:view :mode])
                           :edit (om/build-all edit-count (:counters data))
                           (om/build-all view-count (:counters data))))
                  (dom/h1 nil (name (get-in data [:view :mode])))
                  (dom/h1 nil (get-in data [:view :type]))
                  ;; The button is the from state to routes binding
                  (dom/button #js {:onClick
                                   (fn [_] (om/update! data [:view :mode] :list))}
                              "List")
                  (dom/button #js {:onClick
                                   (fn [_] (om/update! data [:view :mode] :edit))}
                              "Edit")
                  (dom/button #js {:onClick
                                   (fn [_] (om/update! data [:view :mode] :view))}
                              "View")
                  ;; The links are the routes to state binding
                  (dom/br nil)
                  (dom/a #js {:href "#list/0"} "List")
                  (dom/a #js {:href "#edit/0"} "Edit")
                  (dom/a #js {:href "#view/0"} "View")))))
   app-state
   {:target (. js/document (getElementById "app"))
    :shared {:tx-chan tx-pub-chan}
    :tx-listen (fn [tx-data root-cursor]
                 (put! tx-chan [tx-data root-cursor]))}))


;; Plugin Secretary to Goog History
(let [h (History.)]
  (goog.events/listen h EventType/NAVIGATE
                      #(secretary/dispatch! (.-token %)))
  (doto h (.setEnabled true)))
