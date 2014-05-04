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
      {:query-chan (chan)
       ; :query will be passed by the parent
       })
    om/IWillMount
    (will-mount [_]
      (let [query-chan (om/get-state owner :query-chan)
            query (om/get-state owner :query)]
        (if (not (and query (= :album (:type query))))
          (println "error, query is nil or not an album query")
          (go
            (storage/find-many query query-chan)
            (let [query-result (<! query-chan)]
              (om/update-state! owner (fn [state] (assoc state :result query-result))))
            ))))
    om/IRenderState
    (render-state [_ {:keys [result]}]
      (when result
        (.forEach result (fn [r] (println (js->clj r :keywordize-keys true)))))
      (dom/div nil (str "I am albums component with state" result ))
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
