(ns routing.example
    (:require-macros [cljs.core.async.macros :refer [go]])
    (:require [om.core :as om :include-macros true]
              [cljs.core.async :as async :refer [put! chan]]
              [routing.core :as routing]
              [om.dom :as dom :include-macros true]))

(enable-console-print!)

(defonce app-state (atom {:nav {:method :state}}))

(defn nav-to [view-cursor method]
  (om/update! view-cursor [:nav :method] method :routing.core/nav))

(defn get-nav [data]
  (get-in data [:nav :method]))

(defn view-component [data owner]
  (om/component
   (dom/div nil
            (dom/h1 nil (name (get-nav data)))
            ;; The button is the from state to routes binding
            (dom/button #js {:onClick (fn [_] (nav-to data :state))}
                        "Button") 
            ;; The links are the routes to state binding
            (dom/br nil)
            (dom/a #js {:href "#link"} "Link"))))

;; Things for the API

(defn url->state [{:keys [method]}]
  {:method (keyword method)})

(def cursor-path :nav)

(def route [["#" :method] (routing/make-handler url->state)])

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
