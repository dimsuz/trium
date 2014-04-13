(ns trium.dom-utils
  (:require [goog.style :as style]
            [goog.dom :as dom]))

(defn by-id [id]
  (. js/document (getElementById id)))

(defn height [element]
  (let [size (style/getSize element)]
    (.-height size)))
