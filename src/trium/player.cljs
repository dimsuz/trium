(ns trium.player
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <! timeout]]))

(defn play [track comm]
  (go
    (<! (timeout 1000))
    (put! comm [:playing]))
  comm)

(defn pause [comm]
  (go
    (<! (timeout 1000))
    (put! comm [:paused]))
  comm)
