(ns ^:figwheel-always routing.example
    (:require-macros [cljs.core.async.macros :refer [go]])
    (:require [om.core :as om :include-macros true]
              [cljs.core.async :as async :refer [put! chan alts!]]
              [routing.core :as routing]
              [om.dom :as dom :include-macros true]))

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
  (om/update! view-cursor [:view :mode] mode :routing.core/nav))

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

(defn url->state [{:keys [mode type]}]
  {:mode (keyword mode)
   :type (js/parseInt type)})

(def cursor-path :view)

;; route and handler-map should be merged

(def route [[:mode "/" :type] :handler])

(def handler-map {:handler url->state})

;; API

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
                           :handler-map handler-map
                           :korks cursor-path}}))))
   app-state
   {:target (. js/document (getElementById "app"))
    :shared {:tx-chan tx-pub-chan}
    :tx-listen (fn [tx-data root-cursor]
                 (put! tx-chan [tx-data root-cursor]))}))


;; Plugin Secretary to Goog History

