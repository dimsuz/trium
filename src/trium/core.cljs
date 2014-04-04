(ns trium.core
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(enable-console-print!)

(def gui-data
  {:left-sidebar {:items [{:title "MAIN" :type :header}
                          {:title "Play Queue"}
                          {:title "News"}
                          {:title "COLLECTION" :type :header}
                          {:title "Library"}
                          {:title "Favorites"}
                          {:title "History"}
                          {:title "OTHER" :type :header}
                          {:title "Local files"}
                          ]}})

(def app-state
  (atom
   {:ui gui-data}
   {:text "Hello, Trium"}))

(defn sidebar-item [item owner]
  (reify
    om/IRender
    (render [_]
      (dom/li (when (= :header (:type item)) #js {:className "uk-nav-header"})
              (dom/a nil (:title item))))))

(defn sidebar-view [sidebar owner]
  (reify
    om/IRender
    (render [_]
      (apply dom/ul #js {:className "uk-nav"}
             (om/build-all sidebar-item (:items sidebar))))))

(defn app-ui [app owner]
  (sidebar-view (get-in app [:ui :left-sidebar])))

(om/root app-ui app-state
         {:target (. js/document (getElementById "testview"))})
