(ns trium.queue
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [cljs.core.async :refer [put! chan <! timeout]]
            [goog.style :as gstyle]))

(defn gsize->vec [size]
  [(.-width size) (.-height size)])

(defn queue-table-headers []
  (map :title (:queue-headers trium.core/gui-data)))

(defn queue-table-track-fields []
  (map :track-field (:queue-headers trium.core/gui-data)))

(defn queue-row-component [[track current-track] owner]
  (reify
    om/IRender
    (render [_]
      ;; select row must have tr with 'positive' class and first 'td' inside too (to show vertical bar)
      (let [fields (queue-table-track-fields)
            make-cell (fn [field selected?]
                            (dom/td (when selected? #js {:className "positive"}) (field track)))
            selected? (and (not (nil? (:id current-track))) (= (:id current-track) (:id track)))]
        (apply dom/tr (when selected? #js {:className "positive"})
               (make-cell (first fields) selected?)
               (map #(make-cell % false) (rest fields)))))))

(defn build-headers []
  (dom/thead nil
             (apply dom/tr nil
                    (map #(dom/th nil %) (queue-table-headers)))))

(defn build-tracks [app]
  (apply dom/tbody nil
         (map #(om/build queue-row-component [% (:current-track app)]) (get-in app [:queue :tracks]))))

(defn build-navbar-item [item-data]
  (dom/div #js {:className (if (:active item-data) "ui active step" "ui step")}
           (:title item-data)))

(defn queue-navbar-component [navbar owner]
  (reify
    om/IDidMount
    (did-mount [_]
      (let [height (-> (om/get-node owner "queue-navbar")
                            gstyle/getSize gsize->vec second)
            height-chan (om/get-state owner :height-chan)]
        (put! height-chan height)))
    om/IRender
    (render [_]
      (apply dom/div #js {:className "ui four small steps queue-navbar" :ref "queue-navbar"}
             ;; add a fake item so that semantic css would display arrow after last item
             ;; TODO better solve this with css
             (map build-navbar-item (conj navbar {:title "" :fake true}))))))

(defn queue-component [app owner]
  (reify
    om/IInitState
    (init-state [_] {:component-height 200
                     :header-height 0
                     :navbar-height 0
                     :nav-height-chan (chan)})
    om/IWillMount
    (will-mount [_]
      (let [height-chan (om/get-state owner :nav-height-chan)]
        (go
          ;; wait until size of outer navbar component is available
          (om/set-state! owner :navbar-height (<! height-chan)))))
    om/IDidMount
    (did-mount [_]
      (let [header-height (-> (om/get-node owner "queue-header")
                            gstyle/getSize gsize->vec second)
            component-height (-> (om/get-node owner "queue-component")
                            gstyle/getSize gsize->vec second)]
        ;; navbar height will come through chan, cannot get it through ref, navbar is independent component
        (println "mounting")
        (om/set-state! owner :component-height component-height)
        (om/set-state! owner :header-height header-height)))
    om/IRenderState
    (render-state [_ state]
      (let [content-height (- (:component-height state)
                              (+ (:header-height state) (:navbar-height state)))]
        (dom/div #js {:className "queue-component" :ref "queue-component"}
                 (dom/table #js {:className "ui table" :ref "queue-header"}
                            (build-headers))
                        (dom/div #js {:className "queue-tracks"
                               :style #js {:height content-height}}
                          (dom/table #js {:className "ui table"}
                                     (build-tracks app)))
                 (om/build queue-navbar-component
                           (get-in app [:queue :navbar])
                           {:init-state {:height-chan (:nav-height-chan state)}}))))))
