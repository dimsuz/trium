(ns trium.queue
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn queue-table-headers []
  (map :title (:queue-headers trium.core/gui-data)))

(defn queue-table-track-fields []
  (map :track-field (:queue-headers trium.core/gui-data)))

(defn queue-row [[track current-track] owner]
  (reify
    om/IRender
    (render [_]
      ;; select row must have tr with 'positive' class and first 'td' inside too (to show vertical bar)
      (let [fields (queue-table-track-fields)
            make-cell (fn [field selected?]
                            (dom/td (when selected? #js {:className "positive"}) (field track)))
            selected? (and (not (nil? (:id current-track))) (= (:id current-track) (:id track)))
            ]
        (apply dom/tr (when selected? #js {:className "positive"})
               (make-cell (first fields) selected?)
               (map #(make-cell % false) (rest fields)))))))

(defn build-headers []
  (dom/thead nil
             (apply dom/tr nil
                    (map #(dom/th nil %) (queue-table-headers)))))

(defn build-tracks [app]
  (apply dom/tbody nil
         (map #(om/build queue-row [% (:current-track app)]) (get-in app [:queue :tracks]))))

(defn queue-view [app]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/table #js {:className "ui table"}
                          (build-headers)
                          (build-tracks app))
               (dom/div #js {:className "ui four small steps"}
                        (dom/div #js {:className "ui active step"} "Abakus - Shared Light")
                        (dom/div #js {:className "ui step"} "Abakus II")
                        (dom/div #js {:className "ui step"} nil))))))
