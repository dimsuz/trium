(ns trium.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

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
                          ]}})

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

(defn playback-controls-view [app]
  (dom/div #js {:className "uk-panel uk-panel-box"}
           (dom/a #js {:href "#" :className "uk-icon-button uk-icon-backward"} nil)
           (dom/a #js {:href "#" :className "uk-icon-button uk-icon-play"} nil)
           (dom/a #js {:href "#" :className "uk-icon-button uk-icon-forward"} nil)))

(defn trium-app [app owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "uk-grid"}
               (dom/div #js {:id "main-sidebar" :className "uk-width-1-5"} (left-sidebar app))
               (dom/div #js {:id "center-panel" :className "uk-width-4-5 uk-panel uk-panel-box"} (queue-view app))
               (dom/div #js {:className "uk-width-1-1"} (playback-controls-view app)))))
    )

(om/root trium-app app-state
         {:target (. js/document (getElementById "app"))})
