(ns trium.library
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <! timeout]]
            [trium.storage :as storage]))

(defn album-component [album owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "item album-card"}
               (dom/div #js {:className "image"}
                      (dom/img #js {:className "ui medium image" :src "images/placeholder_600x400.svg" :alt ""}))
               (dom/div #js {:className "content"}
                        (dom/div #js {:className "name"} (:artist album))
                        (dom/div nil (:name album)))))))

(defn albums-component [app owner]
  (reify
    om/IRender
    (render [_]
      (apply dom/div #js {:className "ui three items"}
             (om/build-all album-component (sort-by :artist (:query-result app))))
      )
    )
  )

(defn library-component [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className ""}
               (om/build albums-component app {:init-state {:query {:type :album}}})
               ))))
