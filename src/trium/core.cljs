(ns trium.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <! timeout]]
            [trium.player :as player]
            [trium.anim-utils :as anim]
            [trium.dom-utils :as dom-utils]
            [trium.storage :as storage]
            [trium.library :as library]
            [trium.queue :as queue])
  )

(enable-console-print!)

(def gui-data
  {:left-sidebar {:items [{:title "MAIN" :type :header}
                          {:title "Play Queue" :id :queue :icon "unordered list"}
                          {:title "News" :icon "rss" :badge "3"}
                          {:title "COLLECTION" :type :header}
                          {:title "Library" :id :library :icon "folder"}
                          {:title "Favorites" :id :favorites :icon "star"}
                          {:title "History" :id :history :icon "time"}
                          {:title "Files" :id :files :icon "open folder"}
                          {:title "PLAYLISTS" :type :header}
                          {:title "Chillout" :icon "music"}
                          {:title "Jazz" :icon "music"}
                          {:title "Ambient" :icon "music"}
                          ]}
   :queue-headers [{:title "Artist" :track-field :artist}
                   {:title "Title" :track-field :title}
                   {:title "Album" :track-field :album}]})

(def test-tracks [{:title "Shared Light" :artist "Abakus" :file "/home/dimka/Music/Abakus/That Much Closer to the Sun/02 Shared Light.mp3"}
                  {:title "Shared Light II" :artist "Abakus II" :file "/home/dimka/Music/Abakus/That Much Closer to the Sun/02 Shared Light.mp3"}
                  {:title "Shared Light II" :artist "Abakus II" :file "/home/dimka/Music/Abakus/That Much Closer to the Sun/02 Shared Light.mp3"}
                  {:title "Shared Light II" :artist "Abakus II" :file "/home/dimka/Music/Abakus/That Much Closer to the Sun/02 Shared Light.mp3"}
                  {:title "Shared Light II" :artist "Abakus II" :file "/home/dimka/Music/Abakus/That Much Closer to the Sun/02 Shared Light.mp3"}
                  {:title "Shared Light II" :artist "Abakus II" :file "/home/dimka/Music/Abakus/That Much Closer to the Sun/02 Shared Light.mp3"}
                  {:title "Shared Light II" :artist "Abakus II" :file "/home/dimka/Music/Abakus/That Much Closer to the Sun/02 Shared Light.mp3"}
                  {:title "Shared Light II" :artist "Abakus II" :file "/home/dimka/Music/Abakus/That Much Closer to the Sun/02 Shared Light.mp3"}
                  {:title "Shared Light II" :artist "Abakus II" :file "/home/dimka/Music/Abakus/That Much Closer to the Sun/02 Shared Light.mp3"}
                  {:title "Shared Light II" :artist "Abakus II" :file "/home/dimka/Music/Abakus/That Much Closer to the Sun/02 Shared Light.mp3"}
                  {:title "Shared Light II" :artist "Abakus II" :file "/home/dimka/Music/Abakus/That Much Closer to the Sun/02 Shared Light.mp3"}
                  {:title "Shared Light II" :artist "Abakus II" :file "/home/dimka/Music/Abakus/That Much Closer to the Sun/02 Shared Light.mp3"}
                  {:title "Shared Light II" :artist "Abakus II" :file "/home/dimka/Music/Abakus/That Much Closer to the Sun/02 Shared Light.mp3"}
                  {:title "Shared Light II" :artist "Abakus II" :file "/home/dimka/Music/Abakus/That Much Closer to the Sun/02 Shared Light.mp3"}
                  {:title "Shared Light II" :artist "Abakus II" :file "/home/dimka/Music/Abakus/That Much Closer to the Sun/02 Shared Light.mp3"}])

(def app-state
  (atom
   {:queue {:tracks test-tracks
            :navbar [{:title "Abakus - Shared Light" :active true}
                     {:title "Burial - Untrue"}]}
    ;; possible values: :playing :paused
    :player-state :paused
    :current-track nil
    :current-notification nil
    :selected-section :queue
    }))

(defn sidebar-item [{:keys [title icon type badge id active]} owner]
  (reify
    om/IRender
    (render [_]
      (if (= :header type)
        (dom/div #js {:className "item header"} title)
        (dom/a #js {:className (if active "active item" "item")
                    :onClick (fn [e]
                               (when id
                                 (let [comm (om/get-shared owner :comm)]
                                   (put! comm [:section-change id]))))}
               title
               (when icon (dom/i #js {:className (str icon " icon left")} nil))
               (when badge (dom/div #js {:className "ui label"} badge)))))))

(defn playpause-button [app owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [comm]}]
      (let [paused? (= :paused (:player-state app))
            icon (if paused? "icon play" "icon pause")
            command (if paused? :play :pause)]
        (dom/div #js {:className "ui circular icon button "
                    :onClick (fn [e] (put! comm [:playback-command command]))}
                 (dom/i #js {:className icon} nil))))))

(defn forward-button [app owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [comm]}]
      (dom/div #js {:href "#" :className "ui small circular icon button"
                  :onClick (fn [e] (put! comm [:playback-command :forward]))}
               (dom/i #js {:className "icon forward"} nil)))))

(defn backward-button [app owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [comm]}]
      (dom/a #js {:href "#" :className "ui small circular icon button"
                  :onClick (fn [e] (put! comm [:playback-command :backward]))}
             (dom/i #js {:className "icon backward"} nil)))))


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
               (dom/div #js {:className "notification-inner ui segment"}
                        (:text notification)
                        (dom/div #js {:className "ui blue active progress"}
                                 (dom/div #js {:className "bar" :style #js {:width "100%"}} nil))
                        (dom/button #js {:className "ui tiny button"} "Wrong folder, choose another")
                        )))))

(defn playback-panel [app owner]
  (reify
    om/IRender
    (render [_]
      (let [comm (om/get-shared owner :comm)
            local-state {:init-state {:comm comm}}]
        (dom/div #js {:id "playback-panel" :className "sixteen wide column"}
                 (dom/div #js {:className "ui segment"}
                          (om/build backward-button app local-state)
                          (om/build playpause-button app local-state)
                          (om/build forward-button app local-state)
                          (om/build current-track-view (:current-track app))))))))

(defn build-library-section [app]
  [(om/build library/library-component app)])

(defn build-queue-section [app]
  [(om/build queue/queue-component app)
   (om/build queue/queue-navbar-component (get-in app [:queue :navbar]))])

(defn central-panel [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:id "center-panel" :className "twelve wide column"}
               (dom/div #js {:className "center-panel-content"}
                        (apply dom/div #js {:className "scrollable-area ui segment"}
                                 (condp = (:selected-section app)
                                   :library
                                   (build-library-section app)

                                   :queue
                                   (build-queue-section app)
                                   )))
               (when-let [n (:current-notification app)]
                 (om/build notification-view n {:init-state (select-keys n [:comm])})))
      )))

(defn left-sidebar [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:id "main-sidebar" :className "four wide column"}
               (apply dom/div #js {:id "main-sidebar" :className "ui fluid vertical menu"}
                        (om/build-all sidebar-item
                                      (map
                                       #(assoc %
                                          :active (= (:selected-section app) (:id %)))
                                       (get-in gui-data [:left-sidebar :items]))))))))

(defn playpause-playback [app cmd]
  "Starts playing or pauses depending on current player state.
 Returns a channel to which a new player state will be put when it changes"

  (if-let [notification (:current-notification @app)]
    (put! (:comm notification) :close)
    (om/transact! app (fn [app] (let [notify-comm (chan)]
                                  (assoc app :current-notification {:text "Detected music folder: '/home/dimka/Music', scanning it..." :comm notify-comm}))))
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

(defn handle-section-change-cmd [app cmd]
  (om/transact! app #(assoc % :selected-section cmd))
  (if (= :library cmd)
    (om/transact! app #(assoc % :query-result (storage/get-albums []))))
  )

(defn handle-event [app type value]
  (cond
   (= type :playback-command) (handle-playback-cmd app value)
   (= type :section-change) (handle-section-change-cmd app value)
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
      (dom/div #js {:className "ui grid"}
               (om/build left-sidebar app)
               (om/build central-panel app)
               (om/build playback-panel app)
               )))
    )

(storage/create-and-fill-database)
(player/init)
(om/root trium-app app-state
         {:target (dom-utils/by-id "app")
          :shared {:comm (chan)}})
