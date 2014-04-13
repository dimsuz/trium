(ns trium.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <! timeout]]
            [trium.player :as player]
            [trium.anim-utils :as anim]
            [trium.dom-utils :as dom-utils])
  )

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
    :current-track nil
    :current-notification nil
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


(defn current-track-view [track owner]
  (reify
    om/IRender
    (render [_]
      (dom/span nil (if track
                      (str (:title track) " "(:duration track))
                     "No track playing")))))

(defn notification-view [notification owner]
  (reify
    om/IInitState
    (init-state [_]
      {:y (- 800) :alpha 0 :id "notification-1"})
    om/IDidMount
    (did-mount [_]
      ;; note that height calculation depens on element being in dom => IDidMount usage
      (let [height (dom-utils/height (dom-utils/by-id (om/get-state owner :id)))
            hidden [(- height) 0]
            visible [0 1]
            duration 300
            anim-fn (fn [type [y alpha]]
                      (when (= type :animate)
                        (om/set-state! owner :y y)
                        (om/set-state! owner  :alpha alpha)))]
        (anim/animate hidden visible duration :easeOut anim-fn)
        (let [comm (om/get-state owner :comm)]
          (go (loop []
                (let [command (<! comm)]
                  ;; other possibilities for future - minimize... ?
                  (when (= :close command)
                    (anim/animate visible hidden duration :easeOut
                                  (fn [t v]
                                    (anim-fn t v)
                                    (when (= :end t)
                                      (om/update! notification nil))))))
                (recur))))))
    om/IRenderState
    (render-state [this state]
      (dom/div #js {:id (:id state) :className "notification" :style #js {:bottom (str (:y state) "px")
                                                                          :opacity (:alpha state)
                                                                          }}
               (dom/div #js {:className "notification-inner uk-panel uk-panel-box"} (:text notification))))))

(defn playback-panel [app owner]
  (reify
    om/IRender
    (render [_]
      (let [comm (om/get-shared owner :comm)
            local-state {:init-state {:comm comm}}]
        (dom/div #js {:className "uk-width-1-1"}
                 (dom/div #js {:className "uk-panel uk-panel-box"}
                          (om/build backward-button app local-state)
                          (om/build playpause-button app local-state)
                          (om/build forward-button app local-state)
                          (om/build current-track-view (:current-track app))))))))

(defn central-panel [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:id "center-panel" :className "uk-width-4-5"}
               (dom/div #js {:className "center-panel-content"}
                        (queue-view app))
               (when-let [n (:current-notification app)]
                 (om/build notification-view n {:init-state (select-keys n [:comm])})))
      )))

(defn left-sidebar [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:id "main-sidebar" :className "uk-width-1-5"}
               (dom/div #js {:className "uk-panel uk-panel-box"}
                        (apply dom/ul #js {:className "uk-nav uk-nav-side"}
                               (om/build-all sidebar-item (get-in gui-data [:left-sidebar :items]))))))))

(defn playpause-playback [app cmd]
  "Starts playing or pauses depending on current player state.
 Returns a channel to which a new player state will be put when it changes"

  (if-let [notification (:current-notification @app)]
    (put! (:comm notification) :close)
    (om/transact! app (fn [app] (let [notify-comm (chan)]
                                  (assoc app :current-notification {:text "Ennui art party freegan stumptown deep v disrupt. Kogi brunch mumblecore, Pitchfork pop-up sartorial chia bicycle rights Banksy twee bespoke. Cliche put a bird on it Neutra chillwave. Whatever hella American Apparel gastropub bitters. Art party wolf tote bag, cardigan asymmetrical Truffaut messenger bag put a bird on it deep v selvage meggings leggings. Cardigan pop-up kale chips tousled, single-origin coffee scenester biodiesel polaroid Vice drinking vinegar Pinterest pork belly kitsch Wes Anderson. 90's American Apparel post-ironic, squid biodiesel normcore Bushwick trust fund 8-bit Neutra cardigan food truck." :comm notify-comm}))))
    )
  (let [comm (chan)]
    (if (= :play cmd)
      (player/play {:title "Track" :file "/home/file.mp3"} comm)
      (player/pause comm))))

(defn handle-playback-cmd [app cmd]
  (cond
   (or (= :play cmd) (= :pause cmd))
   (let [ch (playpause-playback app cmd)]
     ;; change player state right away and then wait for track to be
     ;; loaded and details to arrive
     (om/transact! app (fn [app] (assoc app :player-state
                                        (if (= :paused (:player-state app)) :playing :paused))))
     (go
       (let [[new-state track] (<! ch)]
         (om/transact! app
                       (fn [app]
                         (if (= :playing new-state)
                           (assoc app :current-track track)
                           (assoc app :current-track nil)))))))
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
    om/IWillMount
    (will-mount [_]
      (let [comm (om/get-shared owner :comm)]
        (go (loop []
              (apply handle-event app (<! comm))
              (recur)))))
    om/IRender
    (render [_]
      (dom/div #js {:className "uk-grid"}
               (om/build left-sidebar app)
               (om/build central-panel app)
               (om/build playback-panel app))))
    )

;(player/init)
(om/root trium-app app-state
         {:target (dom-utils/by-id "app")
          :shared {:comm (chan)}})
