(ns trium.player
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <! timeout]]))

(declare handleLoad)

(def Sound (.-Sound js/createjs))

(defn init []
  (.addEventListener Sound "fileload" handleLoad))

(defn handleLoad [e]
  (prn "handled load" e))

(defn play [track comm]
  (go
    ;(<! (timeout 1000))
    (.registerSound Sound (str "file://" (:source track)) "sound_id")
    (.play Sound "sound_id")
    (put! comm [:playing (assoc track :duration "02:55")]))
  comm)

(defn pause [comm]
  (go
    ;(<! (timeout 1000))
    (.stop Sound)
    (put! comm [:paused]))
  comm)

(defn stop [comm]
  (go
    ;(<! (timeout 1000))
    (.stop Sound)
    (put! comm [:stopped]))
  comm)
