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
    (go
      ;; first stop what is playing.
      ;; TODO correctly update app state! because if loading or playing then fails, app
      ;; needs to be in not-playing state
      (<! (player/stop (chan)))
      ;; TODO move this (and stopping) inside player/play and player/stop.
      ;; let it use Sound.js or whatever it uses internally and let it update
      ;; app state there too
      (let [[state track] (<! (player/play (first tracks) (chan)))]
        (om/transact! app
                      (fn [app]
                        (if (= :playing state)
                          (assoc app :current-track track :player-state :playing)
                          (assoc app :current-track nil))))))
    )
  )
