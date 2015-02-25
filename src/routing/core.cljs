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

(defonce app-state (atom {:view {:edit? false
                                 :type 0}
                          :type [{:count 0}
                                 {:count 0}]}))

(defn view-count [data owner]
  (om/component
   (dom/div nil
            (:count data))))

(defn edit-count [data owner]
  (om/component
   (dom/button #js {:onClick (fn [_] (om/transact! data :count inc))}
               (:count data))))

(defroute "/edit" {:as params}
  (println "dispatching view")
  (swap! app-state #(assoc-in % [:view :edit?] true)))

(defroute "/view" {:as params}
  (println "dispatching view")
  (swap! app-state #(assoc-in % [:view :edit?] false)))

(defroute "/next" {:as params}
  (println "next")
  (swap! app-state #(update-in % [:type 0 :count] inc)))

(defn go-to
  "Goes to the specified url"
  [url]
  (.assign (.-location js/window) "#next") )

(let [tx-chan (chan)
      tx-pub-chan (async/pub tx-chan (fn [_] :txs))]
  (om/root
   (fn [data owner]
     (reify
       om/IWillMount
       (will-mount [_]
         ;; listen on cursor
         (let [tx-chan (om/get-shared owner :tx-chan)
               txs (chan)]
           (async/sub tx-chan :txs txs)
           (om/set-state! owner :txs txs)
           (go (loop []
                 (let [[v c] (<! txs)]
                   (println v)
                   (println c))
                 (recur))))
         )
       om/IRender
       (render [_]
         (dom/div nil
                  (apply dom/div nil
                         (if (get-in data [:view :edit?])
                           (om/build-all edit-count (:type data))
                           (om/build-all view-count (:type data))))
                  ;; The button is the from state to routes binding
                  (dom/button #js {:onClick
                                   (fn [_] (om/transact! data [:view :edit?] not))}
                              (if (get-in data [:view :edit?])
                                "View"
                                "Edit"))
                  ;; The links are the routes to state binding
                  (dom/a #js {:href "#view"}
                         "View")
                  (dom/a #js {:href "#edit"}
                         "Edit")
                  (dom/a #js {:onClick
                              (fn [_] (go-to "#next"))}
                         "Next")))))
   app-state
   {:target (. js/document (getElementById "app"))
    :shared {:tx-chan tx-pub-chan}
    :tx-listen (fn [tx-data root-cursor]
                 (put! tx-chan [tx-data root-cursor]))}))


;; Plugin Secretary to Goog History
(let [h (History.)]
  (goog.events/listen h EventType/NAVIGATE #(secretary/dispatch! (.-token %)))
  (doto h (.setEnabled true)))
