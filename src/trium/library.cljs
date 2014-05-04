(ns trium.library
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <! timeout]]
            [trium.storage :as storage]))

(defn albums-component [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:query-chan (chan)})
    om/IWillMount
    (will-mount [_]
      (let [query-chan (om/get-state owner :query-chan)]
        (println "launching query...")
        ((fn []
           (go
             (<! (timeout 3000))
             (println "query completed")
             (put! query-chan [1 3]))))
        (go
          (let [query-result (<! query-chan)]
            (om/update-state! owner (fn [state] (assoc state :result query-result))))
          )))
    om/IRenderState
    (render-state [_ state]
      (println "rendering state" (om/get-state owner))
      (dom/div nil (str "I am albums component with state" state))
      )
    )
  )

(defn library-component [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               "I am a library"
               (om/build albums-component app {:init-state {:query {:type :album}}})
               ))))
