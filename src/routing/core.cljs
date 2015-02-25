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
   (dom/div nil
            (:count data))))

(defn edit-count [data owner]
  (om/component
   (dom/button #js {:onClick (fn [_] (om/transact! data :count inc))}
               (:count data))))

(defn state->url [new-state]
  (if (get-in new-state [:view :edit?])
    "#edit"
    "#view"))

(defn url->state [url]
  (keyword url))


;; API

(defroute "/:id" {:as params}
  (println "dispatching view")
  (swap! app-state #(assoc-in % [:view :mode] (url->state (:id params)))))

(defn go-to
  "Goes to the specified url"
  [url]
  (.assign (.-location js/window) url))

(let [tx-chan (chan)
      tx-pub-chan (async/pub tx-chan (fn [_] :txs))]
  (om/root
   (fn [data owner]
     (reify
       om/IWillMount
       (will-mount [_]
         (let [tx-chan (om/get-shared owner :tx-chan)
               txs (chan)]
           (async/sub tx-chan :txs txs)
           (om/set-state! owner :txs txs)
           (go (loop []
                 (let [[v c] (<! txs)
                       {:keys [path new-value new-state]} v]
                   (go-to (state->url new-state))
                   (println new-value)
                   (println c))
                 (recur)))))
       om/IRender
       (render [_]
         (dom/div nil
                  (apply dom/div nil
                         (case (get-in data [:view :mode])
                           :edit (om/build-all edit-count (:counters data))
                           (om/build-all view-count (:counters data))))
                  (dom/h1 nil (str (get-in data [:view :mode])))
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
                  (dom/a #js {:href "#list"} "List")
                  (dom/a #js {:href "#edit"} "Edit")
                  (dom/a #js {:href "#view"} "View")))))
   app-state
   {:target (. js/document (getElementById "app"))
    :shared {:tx-chan tx-pub-chan}
    :tx-listen (fn [tx-data root-cursor]
                 (put! tx-chan [tx-data root-cursor]))}))


;; Plugin Secretary to Goog History
(let [h (History.)]
  (goog.events/listen h EventType/NAVIGATE #(secretary/dispatch!
                                             (do (println "ASD") (.-token %))))
  (doto h (.setEnabled true)))
