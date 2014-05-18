(ns trium.storage
  (:require [trium.utils :refer [find-first]]))

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
                  :source "/home/dimka/Home/dimka/Music/Burial/Untrue/06 Etched Headplate.mp3"}

                 {:title "Some track" :album "Untrue" :artist "not-Burial" :source "/home/dimka/unknown.mp3"}
                 ])

(def db (atom nil))

(defn create-database []
  [])

(defn stringify [obj]
  (.stringify js/JSON obj))
;;[
;; {
;;   :name "Beatles"
;;   :albums [ { :name "White Album"
;;               :tracks [ {:unid "dfjfj" :title "Black Onion"} ]
;;             }
;;           ]
;; }
;;]

(defn create-artist [t]
  {:name (:artist t) :albums []})

(defn create-album [t]
  {:name (:album t) :tracks []})

(defn upsert-in [seq pred keyseq create-elem-fn update-val-fn]
  "Filters elements in seq using pred and then upserts a value of the element's key,
by reaching into it using keyseq. If no element matches pred, it is conj-ed onto seq after
being created by create-elem-fn while values inside elem will be updated using update-val-fn.
create-elem-fn takes no args, update-val-fn - one arg.
Example:
;; (upsert-in [{:a 1 :b [2 1 3]}] #(= (:a %) 1) [:b] hash-map #(map inc %))
;; => [{:a 1 :b [3 2 4]}]
;; (upsert-in [{:b [2 1 3]}] #(= (:a %) 1) [:b] (fn [_] {:c 1}) #(map inc %))
;; => [{:b [3 2 4]} {:c 1}]"
  (if (some pred seq)
    (map (fn [e]
           (if (pred e)
             (update-in e keyseq update-val-fn)
             e))
         seq)
    (conj seq (update-in (create-elem-fn) keyseq update-val-fn))))

(defn insert-track [db t]
  (upsert-in db #(= (:name %) (:artist t))
          [:albums]
          #(create-artist t)
          (fn [albums]
            (upsert-in albums #(= (:name %) (:album t))
                    [:tracks]
                    #(create-album t)
                    (fn [tracks]
                      (conj tracks (select-keys t [:title :source]))))))
)

(defn insert-tracks! [db tracks]
  (go
    ;; (missing-from-db artists/albums will be added to db by resolve! functions)
    (doseq [t tracks]
      ;; insert sequentially - one after another (use <! to wait for insertion to happen)
      (<! (insert-track db ins-chan t)))
    ;; FIXME error checking!!!
    ))

(defn create-and-fill-database []
  (let [db (reset! db-conn (create-database))]
    (insert-tracks! db mock-data)))

;; FIXME temp
(defn test-reinsert-mock-data []
  (insert-tracks! @db-conn mock-data)
  )

;; (query {:group-by :albums}) => {:albums (lazy-seq [{:title "Album1" :cover "/path/to/cover" :tracks (lazy-seq [{:title "track1"}])}
;;                                                    {:title "Album1" :cover "/path/to/cover" :tracks (lazy-seq [{:title "track1"}])}])}
;; (query {:artist "Artist" :group-by :albums}) => {:albums (lazy-seq [])} ; albums of artist only
;; (query {:artist "Artist" :album "Album" :group-by :albums}) => {:albums (lazy-seq [])} ; single albums with album info
;; (query {:artist "Artist" :album "Album" ; no grouping}) => {:tracks (lazy-seq [])} ; signle album in tracks form
