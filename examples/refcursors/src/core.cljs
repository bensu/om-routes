(ns examples.refcursors.core
    (:require-macros [cljs.core.async.macros :refer [go]])
    (:require [om.core :as om :include-macros true]
              [cljs.core.async :as async :refer [put! chan]]
              [om-routes.core :as routes]
              [om.dom :as dom :include-macros true]))

(enable-console-print!)

;; Main State

(defonce app-state (atom {:nav {:last-click nil}}))

(defn nav-cursor []
  (om/ref-cursor (:nav (om/root-cursor app-state))))

;; Navigation API

(def nav-path :nav)

(defn get-nav []
  (:last-click (nav-cursor)))

(def nav-chan
  "Acts both as tx-listen channel and our own channel"
  (chan))

(defn nav-to [method]
  (put! nav-chan [{:new-state {:nav {:last-click method}} 
                   :tag :om-routes.core/nav} 
                  nil])
  (om/update! (nav-cursor) :last-click method))

(defn url->state [{:keys [last-click]}]
  {:last-click (keyword last-click)})

(def route [["#" :last-click] (routes/make-handler url->state)])

;; View

(defn view-component [data owner]
  (om/component
   (dom/div nil
            (dom/h1 nil (case (get-nav)
                          :button "A button got me here"
                          :link "A link got me here"
                          "Who got me here?"))
            (dom/button #js {:onClick (fn [_] (nav-to :button))} "Button") 
            (dom/br nil)
            (dom/a #js {:href "#link"} "Link"))))

;; Main Component

(let [tx-chan nav-chan 
      tx-pub-chan (async/pub tx-chan (fn [_] :txs))]
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
