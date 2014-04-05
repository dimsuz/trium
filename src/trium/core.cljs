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
                          {:title "OTHER" :type :header}
                          {:title "Files" :icon "uk-icon-folder-open"}
                          ]}})

(def app-state
  (atom
   {:ui gui-data}
   {:text "Hello, Trium"}))

(defn make-sidebar-item [{:keys [title icon badge]} item]
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

(defn sidebar-view [sidebar owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "uk-panel uk-panel-box"}
               (apply dom/ul #js {:className "uk-nav uk-nav-side"}
                      (om/build-all sidebar-item (:items sidebar)))))))

(defn app-ui [app owner]
  (sidebar-view (get-in app [:ui :left-sidebar]) owner))

(om/root app-ui app-state
         {:target (. js/document (getElementById "testview"))})
