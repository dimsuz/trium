(ns trium.library
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <! timeout]]
            [trium.storage :as storage]
            [trium.queue :as queue]))

(defn album-component [album owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [playchan]}]
      (dom/div #js {:className "item album-card"}
               (dom/div
                #js {:className "image"
                     :onClick #(put! playchan {:album album})}
                (dom/img #js {:className "ui medium image" :src "images/placeholder_600x400.svg" :alt ""}))
               (dom/div #js {:className "content"}
                        (dom/div #js {:className "name"} (:artist album))
                        (dom/div nil (:name album)))))))

(defn albums-component [app owner]
  (reify
    om/IRenderState
    (render-state [_ {:keys [playchan]}]
      (apply dom/div #js {:className "ui three items"}
             (om/build-all album-component
                           (sort-by :artist (:query-result app))
                           {:init-state {:playchan playchan}})))))

(defn library-component [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:playchan (chan)})
    om/IWillMount
    (will-mount [_]
      (let [playchan (om/get-state owner :playchan)]
        (go
          (while true
            (let [item (<! playchan)
                  album (get-in item [:album :name])
                  artist (get-in item [:album :artist])]
              (queue/replace app (storage/get-album-tracks {:artist artist :album album}) {:play true}))))))
    om/IRenderState
    (render-state [_ {:keys [playchan]}]
      (dom/div #js {:className ""}
               (om/build albums-component
                         app
                         {:init-state {:query {:type :album} :playchan playchan}})))))
