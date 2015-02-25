(ns ^:figwheel-always routing.core
    (:require [om.core :as om :include-macros true]
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

(defroute "/view" {:as params}
  (println "dispatching view")
  (reset! app-state [:view :edit?] false))

(om/root
 (fn [data owner]
   (reify om/IRender
     (render [_]
       (dom/div nil
                (apply dom/div nil
                       (if (get-in data [:view :edit?])
                         (om/build-all view-count (:type data))
                         (om/build-all edit-count (:type data))))
                (dom/button #js {:onClick
                                 (fn [_] (om/transact! data [:view :edit?] not))}
                            (if (get-in data [:view :edit?])
                              "Edit"
                              "View"))
                (dom/a #js {:href "#view"}
                       "Navigate")))))
 app-state
 {:target (. js/document (getElementById "app"))})


(let [h (History.)]
  (goog.events/listen h EventType/NAVIGATE #(secretary/dispatch! (.-token %)))
  (doto h (.setEnabled true)))
