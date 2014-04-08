(ns trium.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <! timeout]]))

(enable-console-print!)

(def gui-data
  {:left-sidebar {:items [{:title "MAIN" :type :header}
                          {:title "Play Queue" :icon "uk-icon-bars"}
                          {:title "News" :icon "uk-icon-rss" :badge "3"}
                          {:title "COLLECTION" :type :header}
                          {:title "Library" :icon "uk-icon-folder"}
                          {:title "Favorites" :icon "uk-icon-star"}
                          {:title "History" :icon "uk-icon-suitcase"}
                          {:title "Files" :icon "uk-icon-folder-open"}
                          {:title "PLAYLISTS" :type :header}
                          {:title "Chillout" :icon "uk-icon-music"}
                          {:title "Jazz" :icon "uk-icon-music"}
                          {:title "Ambient" :icon "uk-icon-music"}
                          {:title "Jazz" :icon "uk-icon-music"}
                          {:title "Jazz" :icon "uk-icon-music"}
                          {:title "Jazz" :icon "uk-icon-music"}
                          {:title "Jazz" :icon "uk-icon-music"}
                          {:title "Jazz" :icon "uk-icon-music"}
                          {:title "Jazz" :icon "uk-icon-music"}
                          {:title "Jazz" :icon "uk-icon-music"}
                          {:title "Jazz" :icon "uk-icon-music"}
                          {:title "Jazz" :icon "uk-icon-music"}
                          {:title "Jazz" :icon "uk-icon-music"}
                          ]}})

(def app-state
  (atom
   {:queue {:tracks [{:title "Abakus - Shared Light" :file "/home/dimka/Music/Abakus/That Much Closer to the Sun/02 Shared Light.mp3"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track3"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"}]}
    ;; possible values: :playing :paused
    :player-state :paused
    }))

(defn make-sidebar-item [{:keys [title icon badge]}]
  (dom/li nil
          (dom/a nil
                 (when icon (dom/i #js {:className icon}))
                 (str " " title " ")
                 (when badge (dom/span #js {:className "uk-badge"} badge)))))

(defn sidebar-item [item owner]
  (reify
    om/IRender
    (render [_]
      (if (= :header (:type item))
        (dom/li #js {:className "uk-nav-header"} (:title item))
        (make-sidebar-item item)))))

(defn sidebar-view [sidebar]
  (dom/div #js {:className "uk-panel uk-panel-box"}
           (apply dom/ul #js {:className "uk-nav uk-nav-side"}
                  (om/build-all sidebar-item (:items sidebar)))))

(defn left-sidebar []
  (sidebar-view (:left-sidebar gui-data)))

(defn queue-row [track owner]
  (reify
    om/IRender
    (render [_]
      (dom/tr nil
              (dom/td nil (:title track))))))

(defn queue-view [app]
  (dom/table #js {:className "uk-table"}
             (apply dom/tbody nil
                    (om/build-all queue-row (get-in app [:queue :tracks])))))

(defn playpause-button [app owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [comm]}]
      (let [paused? (= :paused (:player-state app))
            icon (if paused? "uk-icon-play" "uk-icon-pause")
            command (if paused? :play :pause)]
        (dom/a #js {:href "#" :className (str "uk-icon-button " icon)
                    :onClick (fn [e]
                               (put! comm [:playback-command command]))} nil)))))

(defn forward-button [app owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [comm]}]
      (dom/a #js {:href "#" :className "uk-icon-button uk-icon-forward"
                  :onClick (fn [e] (put! comm [:playback-command :forward]))} nil))))

(defn backward-button [app owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [comm]}]
      (dom/a #js {:href "#" :className "uk-icon-button uk-icon-backward"
                  :onClick (fn [e] (put! comm [:playback-command :backward]))} nil))))

(defn playback-controls-view [app state]
  (dom/div #js {:className "uk-panel uk-panel-box"}
           (om/build backward-button app {:init-state state})
           (om/build playpause-button app {:init-state state})
           (om/build forward-button app {:init-state state})))

;; TODO put into player namespace
(defn player-play [track c]
  (go
    (<! (timeout 1000))
    (put! c [:playing]))
  c)

;; TODO put into player namespace
(defn player-pause [c]
  (go
    (<! (timeout 1000))
    (put! c [:paused]))
  c)

(defn playpause-playback [app cmd]
  "Starts playing or pauses depending on current player state.
 Returns a channel to which a new player state will be put when it changes"
  (let [c (chan)]
    (if (= :play cmd)
      (player-play {:title "Track" :file "/home/file.mp3"} c)
      (player-pause c))))

(defn handle-playback-cmd [app cmd]
  (cond
   (or (= :play cmd) (= :pause cmd))
   (let [ch (playpause-playback @app cmd)]
     (go
       (let [[new-state] (<! ch)]
         (om/transact! app
                       (fn [app]
                         (if (= :playing new-state)
                           (-> app
                               (assoc :player-state :playing))
                           (-> app
                               (assoc :player-state :paused))))))))
   (= :forward cmd) (prn "TODO implement me")
   (= :backward cmd) (prn "TODO implement me")
   )

  )

(defn handle-event [app type value]
  (cond
   (= type :playback-command) (handle-playback-cmd app value)
   :else
   (prn (str "don't know how to handle event " type " - " value))))

(defn trium-app [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:comm (chan)})
    om/IWillMount
    (will-mount [_]
      (let [comm (om/get-state owner :comm)]
        (go (loop []
              (apply handle-event app (<! comm))
              (recur)))))
    om/IRenderState
    (render-state [this state]
      (dom/div #js {:className "uk-grid"}
               (dom/div #js {:id "main-sidebar" :className "uk-width-1-5"} (left-sidebar))
               (dom/div #js {:id "center-panel" :className "uk-width-4-5 uk-panel uk-panel-box"} (queue-view app))
               (dom/div #js {:className "uk-width-1-1"} (playback-controls-view app state)))))
    )

(om/root trium-app app-state
         {:target (. js/document (getElementById "app"))})
