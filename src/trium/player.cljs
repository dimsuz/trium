(ns trium.player
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <! timeout]]))

(declare handleLoad)

(def Sound (.-Sound js/createjs))

(defn init []
  (.addEventListener Sound "fileload" handleLoad)
  (.registerSound Sound "file:///home/dimka/inner_chill.mp3" "sound_id")
  )

(defn handleLoad [e]
  (prn "handled load" e))

(defn play [track comm]
  (go
    ;(<! (timeout 1000))
    (.play Sound "sound_id")
    (put! comm [:playing (assoc track :duration "02:55")]))
  comm)

(defn pause [comm]
  (go
    ;(<! (timeout 1000))
    (.stop Sound)
    (put! comm [:paused]))
  comm)
