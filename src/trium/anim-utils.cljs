(ns trium.anim-utils
  (:require [goog.fx :as fx]
            [goog.fx.easing :as fx-easing]
            [goog.fx.Animation.EventType :as AnimEventType]
            [goog.events :as events])
  (:import [goog.fx Animation]))

(defn to-fx-easing [easing]
  ;; FIXME support others
  (fx-easing/easeIn))

(defn animate [from to duration easing f]
  "Animates a values in 'from' vector to 'to' vector using easing.
'f' is a function of two args: event type (:begin|:end|:animate) and animated values array.
'easing' must be one of :easeIn :easeOut etc"
  (let [anim (Animation. (clj->js from) (clj->js to) duration (to-fx-easing easing))
        ;; also invoke :animate for END (order in this vector matters for subscribe order)
        anim-events [:begin AnimEventType/BEGIN
                     :animate AnimEventType/ANIMATE
                     :animate AnimEventType/END
                     :end AnimEventType/END
                     ]
        listen-fn (fn [event fx-event] (events/listen anim fx-event #(f event [(.-x %) (.-y %)])))]
    (dorun (map #(apply listen-fn %) (partition 2 anim-events)))
    (.play anim))
  )
