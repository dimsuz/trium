(ns trium.player
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [om.core :as om :include-macros true]
   [cljs.core.async :refer [put! chan <! timeout]]))

(def Sound (.-Sound js/createjs))

(defn init []
  )

(defn sound-id [track] (:source track))

(defn handleLoad [app track e]
  (.removeAllEventListeners Sound "fileload")
  (println (str "ready to play " (:title track) ", starting playback"))
  (let [instance (.play Sound (sound-id track))
        playing? (= (.-PLAY_SUCCEEDED Sound) (.-playState instance))]
    (println (str (if playing? "Started playing " "Failed to play ")) track)
    (om/transact! app
                  (fn [app]
                    (if playing?
                      (assoc app :current-track track :player-state :playing)
                      (assoc app :current-track nil :player-state :paused))))))

(defn stop [app]
  (println "Stopping current track")
  (.stop Sound)
  (om/transact! app (fn [app] (assoc app :current-track nil :player-state :paused))))

(defn play [app track]
  (.addEventListener Sound "fileload" (partial handleLoad app track))
  (let [res (.registerSound Sound (str "file://" (:source track)) (sound-id track))]
    (if (= true res)
      (do
        ;; TODO Prettify! document!
        (println "removing")
        (.removeAllSounds Sound)
        (.registerSound Sound (str "file://" (:source track)) (sound-id track))
        (println "registered sound II" res))
      (println "registered sound" res)))
  ;; sound will start playing in handleLoad, after loading complete
)

(defn pause [comm]
  (go
    ;(<! (timeout 1000))
    (.stop Sound)
    (put! comm [:paused]))
  comm)
