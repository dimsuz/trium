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
      (dom/div #js {:className "uk-width-1-3 album-component"}
               (dom/a #js {:className "uk-thumbnail uk-thumbnail-medium"}
                      (dom/img #js {:src "images/placeholder_600x400.svg" :alt ""})
                      (dom/div #js {:className "uk-thumbnail-caption"}
                               (dom/div nil (:artist album))
                               (dom/div nil (:name album))))))))

(defn albums-component [app owner]
  (reify
    om/IRender
    (render [_]
      (apply dom/div #js {:className "uk-grid"}
             (om/build-all album-component (sort-by :artist (:query-result app))))
      )
    )
  )

(defn library-component [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (om/build albums-component app {:init-state {:query {:type :album}}})
               ))))
