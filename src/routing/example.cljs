(ns routing.example
    (:require-macros [cljs.core.async.macros :refer [go]])
    (:require [om.core :as om :include-macros true]
              [cljs.core.async :as async :refer [put! chan]]
              [routing.core :as routing]
              [om.dom :as dom :include-macros true]))

(enable-console-print!)

;; Main State

(defonce app-state (atom {:nav {:method :state}}))

;; Navigation API

(def nav-path :nav)

(defn get-nav [data]
  (get-in data [nav-path :method]))

(defn url->state [{:keys [method]}]
  {:method (keyword method)})

(def route [["#" :method] (routing/make-handler url->state)])

(defn nav-to [view-cursor method]
  (om/update! view-cursor [nav-path :method] method :routing.core/nav))

;; View

(defn view-component [data owner]
  (om/component
   (dom/div nil
            (dom/h1 nil (case (get-nav data)
                          :state "A button got me here"
                          :link "A link got me here"
                          "Who got me here?"))
            (dom/button #js {:onClick (fn [_] (nav-to data :state))} "Button") 
            (dom/br nil)
            (dom/a #js {:href "#link"} "Link"))))

;; Main Component

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
                           :nav-path nav-path}}))))
   app-state
   {:target (. js/document (getElementById "app"))
    :shared {:tx-chan tx-pub-chan}
    :tx-listen (fn [tx-data root-cursor]
                 (put! tx-chan [tx-data root-cursor]))}))
