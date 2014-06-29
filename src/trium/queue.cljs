(ns trium.queue
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

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
         (map #(om/build queue-row-component [% (:current-track app)]) (get-in app [:queue :tracks]))))

(defn build-navbar-item [item-data]
  (dom/div #js {:className (if (:active item-data) "ui active step" "ui step")}
           (:title item-data)))

(defn queue-navbar-component [navbar]
  (reify
    om/IRender
    (render [_]
      (apply dom/div #js {:className "ui four small steps queue-navbar"}
             ;; add a fake item so that semantic css would display arrow after last item
             ;; TODO better solve this with css
             (map build-navbar-item (conj navbar {:title "" :fake true}))))))

(defn queue-component [app]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/table #js {:className "ui table"}
                          (build-headers)
                          (build-tracks app))))))
