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
      (dom/div nil
               (str (:name album) " by "
                    (:artist album))
               )
      )
    ))

(defn albums-component [app owner]
  (reify
    om/IRender
    (render [_]
      (apply dom/div nil (om/build-all album-component (sort-by :artist (:query-result app))))
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
