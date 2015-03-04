(ns routing.example
    (:require-macros [cljs.core.async.macros :refer [go]])
    (:require [om.core :as om :include-macros true]
              [cljs.core.async :as async :refer [put! chan]]
              [routing.core :as routing]
              [om.dom :as dom :include-macros true]))

(enable-console-print!)

(defonce app-state (atom {:nav {:color :red}
                          :count 0}))

(defn nav-to [view-cursor color]
  (om/update! view-cursor [:nav :color] color :routing.core/nav))

(defn get-color [data]
  (get-in data [:nav :color]))

(defn edit-count [data owner]
  (om/component
   (dom/button #js {:onClick (fn [_] (om/transact! data :count inc))
                    :style #js {"backgroundColor" (name (get-color data))}}
               (:count data))))

(defn view-component [data owner]
  (om/component
   (dom/div nil
            (dom/div nil
                     (om/build edit-count data))
            (dom/h1 nil (name (get-color data)))
            ;; The button is the from state to routes binding
            (dom/button #js {:onClick (fn [_] (nav-to data :red))}
                        "Red")
            (dom/button #js {:onClick (fn [_] (nav-to data :blue))}
                        "Blue")
            ;; The links are the routes to state binding
            (dom/br nil)
            (dom/a #js {:href "#red"} "Red")
            (dom/a #js {:href "#blue"} "Blue"))))

;; Things for the API

(defn url->state [{:keys [color]}]
  {:color (keyword color)})

(def cursor-path :nav)

(def route [["#" :color] (routing/make-handler url->state)])

(let [tx-chan (chan)
      tx-pub-chan
      (async/pub tx-chan (fn [_] :txs))]
  (om/root
   (fn [data owner]
     (reify
       om/IRender
       (render [_]
         (om/build routing/om-routes data
                   {:opts {:view view-component
                           :route route
                           :korks cursor-path}}))))
   app-state
   {:target (. js/document (getElementById "app"))
    :shared {:tx-chan tx-pub-chan}
    :tx-listen (fn [tx-data root-cursor]
                 (put! tx-chan [tx-data root-cursor]))}))
