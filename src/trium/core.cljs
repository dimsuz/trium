(ns trium.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <!]]))

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
                          {:title "Jazz" :icon "uk-icon-music"}
                          ]}
   :playback-buttons [{:icon "uk-icon-backward" :command :backward}
                      {:icon "uk-icon-play" :command :play}
                      {:icon "uk-icon-forward" :command :forward}]})

(def app-state
  (atom
   {:ui gui-data
    :queue {:tracks [{:title "Abakus - Shared Light" :file "/home/dimka/Music/Abakus/That Much Closer to the Sun/02 Shared Light.mp3"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track3"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"} {:title "Track"}]}}))

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

(defn left-sidebar [app]
  (sidebar-view (get-in app [:ui :left-sidebar])))

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

(defn playback-control-button [{:keys [icon command]} owner]
  (reify
    om/IRenderState
    (render-state [this {:keys [comm]}]
      (dom/a #js {:href "#" :className (str "uk-icon-button " icon)
                  :onClick (fn [e] (put! comm [:playback-command command]))} nil))))

(defn playback-controls-view [app state]
  (apply dom/div #js {:className "uk-panel uk-panel-box"}
         (om/build-all playback-control-button (get-in app [:ui :playback-buttons]) {:init-state state}))
  )

(defn handle-event [app type value]
  (prn (str "got event " type " - " value)))

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
               (dom/div #js {:id "main-sidebar" :className "uk-width-1-5"} (left-sidebar app))
               (dom/div #js {:id "center-panel" :className "uk-width-4-5 uk-panel uk-panel-box"} (queue-view app))
               (dom/div #js {:className "uk-width-1-1"} (playback-controls-view app state)))))
    )

(om/root trium-app app-state
         {:target (. js/document (getElementById "app"))})
