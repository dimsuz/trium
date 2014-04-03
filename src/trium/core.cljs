(ns trium.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(enable-console-print!)

(def app-state
  (atom
    {:text "Hello, Trium"}))

(defn test-view [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/h2 nil (:text app)))))

(om/root test-view app-state
         {:target (. js/document (getElementById "testview"))})
