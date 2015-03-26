(ns examples.integers.core
    (:require-macros [cljs.core.async.macros :refer [go]])
    (:require [om.core :as om :include-macros true]
              [cljs.core.async :as async :refer [put! chan]]
              [om-routes.core :as routes]
              [om.dom :as dom :include-macros true]))

(enable-console-print!)

;; Main State

(defonce app-state (atom {:nav {:mode :up
                                :index-1 0
                                :index-2 0}}))

;; Navigation API

(def nav-path :nav)

(defn nav-to [view-cursor nav-map]
  (om/update! view-cursor nav-map :om-routes.core/nav))

(defn url->state [{:keys [mode index-1 index-2]}]
  {:mode (keyword mode)
   :index-1 (js/parseInt index-1)
   :index-2 (js/parseInt index-2)})

(def route [["#" :mode "/" :index-1 "/" :index-2]
            (routes/make-handler url->state)])
;; View

(defn view-component [data owner]
  (om/component
   (dom/div nil
            (dom/h1 nil (case (get-in data [nav-path] :mode) 
                          :up "up" 
                          :down "down" 
                          "upwards?"))
            (dom/button #js {:onClick (fn [_] (om/transact! data [nav-path :index-1] inc :om-routes.core/nav))}
                        (get-in data [nav-path :index-1])) 
            (dom/button #js {:onClick (fn [_] (om/transact! data [nav-path :index-2] inc :om-routes.core/nav))}
                        (get-in data [nav-path :index-2])))))

;; Main Component

(let [tx-chan (chan)
      tx-pub-chan
      (async/pub tx-chan (fn [_] :txs))]
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
