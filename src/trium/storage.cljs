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

(defn create-database []
  (let [Datastore (js/require "nedb")]
    (Datastore.)))

(def next-id (atom 0))
(defn gen-id [] (swap! next-id inc))

(defn findOne [db query-req ch]
  (.findOne db (clj->js (:query query-req)) (fn [err doc] (put! ch {:id (:id query-req) :res doc :err err})))
  ch)

;; TODO sink with count
(defn sink
  "Returns an atom containing a vector. Consumes values from channel
  ch and conj's them into the atom."
  [ch]
  (let [a (atom [])]
    (go
      (loop []
        (let [val (<! ch)]
          (when-not (nil? val)
            (swap! a conj val)
            (recur)))))
    @a))

(defn query-req [query]
  (hash-map :id (gen-id) :query query))

(defn artists-of [db tracks]
  (let [queries (map hash-map (repeat :name) (distinct (map :artist tracks)))
        query-reqs (map query-req queries)
        ch (chan)
        outch (chan)
        ]
    (println query-reqs)
    ;; run all queries and close the channel
    (go
      (doseq [req query-reqs]
        (let [res (<! (findOne db req ch))]
          (put! outch (if-let [artist (:res res)] artist :no-artist))))
      (close! ch)
      (close! outch))
    outch))

(defn resolve-artist-id [artist]
  "given an {:name '...'} map adds an :id key to it with DB id or nil if missing in DB.
Returns a channel from which a resulting map can be read on completion"

  )

(defn add-missing-artists [artists]
  (let [c (chan)]
    )
  )

(defn get-artists [tracks]
  (distinct (map (fn [t]
                   (-> t
                       (select-keys [:artist])
                       (rename-keys {:artist :name})))
                 tracks)))

(defn insert-tracks! [db tracks]
  ;; goal => loop each distinct artist filter out those which are missing, add them, then proceed with adding
  ;; all tracks, artists will exist 100%
  ;; transform tracks array to array of distinct artist names {:name } {:name }
  ;; inside add-missing-artists:
  ;; doseq resolve-artist-id => chan => [{:name '...' :id '...'} {:name '...' :id :no-id}] => (map< chan) => chan [{:name '...' :id :no-id} {:no-id}] =>

  (let [artists get-artists]
    (go
      ;; reading from chanel here is a noop => just wait until adding is complete
      (<! (add-missing-artists artists))
      ;; TODO actual insert
      )
))

(defn create-and-fill-database []
  (let [db (create-database)
        doc #js { :title "Hello"}]
    ;(.insert db doc #(.find db #js {} (fn [_ docs] (println (JSON/stringify docs)))))
    (insert-tracks! db mock-data)
    ))

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
