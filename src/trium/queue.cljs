(ns trium.queue
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [cljs.core.async :refer [put! chan <! timeout]]
            [trium.player :as player]))

(defn queue-append [queue query title])
(defn queue-resolve [queue start-pos end-pos])

(defn replace [app tracks {:keys [play]}]
  "Replaces queue contents with specified tracks and optionally starts playback"
  (om/update! app [:queue :tracks] tracks)
  (when play
    ;; first stop what is playing.
    (player/stop app)
    (player/play app (first tracks))))
