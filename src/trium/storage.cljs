(ns trium.storage
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [put! chan <! close!]]
            [clojure.set :refer [rename-keys]])
  )

(def mock-data [ {:title "Es un sombrero" :album "Inocencia" :artist "Bosques de mi Mente"
                  :source "/home/dimka/Home/dimka/Music/Bosques de mi Mente/Inocencia/01 Es un sombrero.mp3"}
                 {:title "Dibujame un cordero" :album "Inocencia" :artist "Bosques de mi Mente"
                  :source "/home/dimka/Home/dimka/Music/Bosques de mi Mente/Inocencia/02 Dibujame un cordero.mp3"}
                 {:title "No se ver corderos a través de cajas, habré envejecido" :album "Inocencia" :artist "Bosques de mi Mente"
                  :source "/home/dimka/Home/dimka/Music/Bosques de mi Mente/Inocencia/04 No se ver corderos a través de cajas, habré envejecido.mp3"}
                 {:title "Semillas de Baobab" :album "Inocencia" :artist "Bosques de mi Mente"
                  :source "/home/dimka/Home/dimka/Music/Bosques de mi Mente/Inocencia/05 Semillas de Baobab.mp3"}
                 {:title "La suavidad de la puesta de sol" :album "Inocencia" :artist "Bosques de mi Mente"
                  :source "/home/dimka/Home/dimka/Music/Bosques de mi Mente/Inocencia/06 La suavidad de la puesta de sol.mp3"}
                 {:title "Archangel" :album "Untrue" :artist "Burial"
                  :source "/home/dimka/Home/dimka/Music/Burial/Untrue/02 Archangel.mp3"}
                 {:title "Near Dark" :album "Untrue" :artist "Burial"
                  :source "/home/dimka/Home/dimka/Music/Burial/Untrue/03 Near Dark.mp3"}
                 {:title "Ghost Hardware" :album "Untrue" :artist "Burial"
                  :source "/home/dimka/Home/dimka/Music/Burial/Untrue/04 Ghost Hardware.mp3"}
                 {:title "Etched Headplate" :album "Untrue" :artist "Burial"
                  :source "/home/dimka/Home/dimka/Music/Burial/Untrue/06 Etched Headplate.mp3"}])

;; due to having two different JS contexts: nodejs and webkit's, types
;; of Objects do not match between them. So extending this protocol so that
;; js->clj correctly recognizes end encodes Objects created in webkit context.
;; this is mostly the copy of cljs.core (js->clj) function
(extend-protocol IEncodeClojure
  object
  (-js->clj
    ([x {:keys [keywordize-keys] :as options}]
       (let [keyfn (if keywordize-keys keyword str)
             f (fn thisfn [x]
                 (cond
                  (seq? x)
                  (doall (map thisfn x))

                  (coll? x)
                  (into (empty x) (map thisfn x))

                  (array? x)
                  (vec (map thisfn x))

                  ;; this is the changed part - added 'or'
                  (or (identical? (type x) js/Object)
                      (identical? (type x) (.-Object js/global)))
                  (into {} (for [k (js-keys x)]
                             [(keyfn k) (thisfn (aget x k))]))

                  :else x))]
         (f x)))
    ([x] (-js->clj x {:keywordize-keys false}))))

(def db-conn (atom nil))

(defn create-database []
  (let [Datastore (js/require "nedb")]
    (Datastore.)))

(def next-id (atom 0))
(defn gen-id [] (swap! next-id inc))

(defn stringify [obj]
  (.stringify js/JSON obj))

(defn db-find-one [db ch query-req]
  (.findOne db (clj->js (:query query-req)) (fn [err doc] (put! ch
                                                                {:id (:id query-req)
                                                                 :res (js->clj doc :keywordize-keys true)
                                                                 :err err})))
  ch)

(defn db-insert [db ch record]
  (.insert db (clj->js record) (fn [err doc]
                                  (if err
                                    (put! ch {:error (str "failed to insert record " record ", error " err)})
                                    (put! ch (js->clj doc :keywordize-keys true)))))
  ch)

(defn query-req [query]
  (hash-map :id (gen-id) :query query))

;; FIXME remove this, is it used?
(defn artists-of [db tracks]
  (let [queries (map hash-map (repeat :name) (distinct (map :artist tracks)))
        query-reqs (map query-req queries)
        ch (chan)
        outch (chan)
        ]
    ;; run all queries and close the channel
    (go
      (doseq [req query-reqs]
        (let [res (<! (db-find-one db ch req))]
          (put! outch (if-let [artist (:res res)] artist :no-artist))))
      (close! ch)
      (close! outch))
    outch))

(defn resolve-artist [db ch artist]
  "given an {:name '...'} map adds an :id key to it with DB id or nil if missing in DB.
Returns a channel from which a resulting map can be read on completion"
  (go
    (let [query-res (<! (db-find-one db (chan) (query-req artist)))]
      (if-let [err (:error query-res)]
        (put! ch {:error (str "failed to query for artist " (:name artist) ", " err)})
        (if-let [a (:res query-res)]
          (put! ch a) ;; put artist as it is in db (with :id)
          (put! ch artist) ;; put original artist data, unresolved
          ))))
  ch)


(defn resolve-artists [db artists]
  (let [c (chan)
        out-ch (chan)]
    (go
      (doseq [a artists]
        (let [ra (<! (resolve-artist db c a))]
          (when (:error ra) (println (str "warning! error while searching for artist "
                                          a ", will try to insert it anyway")))
          (if (:_id ra)
            (put! out-ch ra)
            (put! out-ch (<! (db-insert db c a))))))
      (close! out-ch))
    (async/into [] out-ch)))

(defn get-distinct-artists [tracks]
  "Returns a seq of artist data, like {:name '...', ...}"
  (distinct (map (fn [t]
                   (-> t
                       (select-keys [:artist])
                       (rename-keys {:artist :name})))
                 tracks)))

(defn artists-to-name-id-map [artists]
  "Takes a seq of maps [{:name 'artist1' :id 'id1'} ...], returns a joined
map of {'artist1' => 'id1', 'artist2' => 'id2'}"
  (apply merge (map #(apply hash-map (vals %)) artists)))

(defn resolve-track-links [tracks artists-map]
  (map #(assoc % :artist (get artists-map (:artist %))) tracks)
  )

(defn insert-tracks! [db tracks]
  (go
    (let [artists (get-distinct-artists tracks)
          aname-id-map (artists-to-name-id-map (<! (resolve-artists db artists)))]
      (println (resolve-track-links tracks aname-id-map))))
  )

(defn create-and-fill-database []
  (let [db (reset! db-conn (create-database))
        doc #js { :title "Hello"}]
    ;(.insert db doc #(.find db #js {} (fn [_ docs] (println (JSON/stringify docs)))))
    (insert-tracks! db mock-data)
    ))

;; FIXME temp
(defn test-reinsert-mock-data []
  (insert-tracks! @db-conn mock-data)
  )

;; (query {:group-by :albums}) => {:albums (lazy-seq [{:title "Album1" :cover "/path/to/cover" :tracks (lazy-seq [{:title "track1"}])}
;;                                                    {:title "Album1" :cover "/path/to/cover" :tracks (lazy-seq [{:title "track1"}])}])}
;; (query {:artist "Artist" :group-by :albums}) => {:albums (lazy-seq [])} ; albums of artist only
;; (query {:artist "Artist" :album "Album" :group-by :albums}) => {:albums (lazy-seq [])} ; single albums with album info
;; (query {:artist "Artist" :album "Album" ; no grouping}) => {:tracks (lazy-seq [])} ; signle album in tracks form

(defn query [q]
  ;; currently returns mocked data
  ;; (if-let [artist (:artist q)]
  ;;   (case (:artist q))
  ;;   ;; no query given
  ;;   (merge-with #(concat %1 %2) (query {:artist "Burial"}) (query {:artist "Bosques de mi Mente"})))
  )
