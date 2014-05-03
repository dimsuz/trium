(ns trium.storage
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :as async :refer [put! chan <! close!]]
            [clojure.set :refer [rename-keys]]
            [trium.utils :refer [find-first]])
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
                  :source "/home/dimka/Home/dimka/Music/Burial/Untrue/06 Etched Headplate.mp3"}

                 {:title "Some track" :album "Untrue" :artist "not-Burial" :source "/home/dimka/unknown.mp3"}
                 ])

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
  "A generic version of DB query which labels each result with :id which can be used in certain parallel scenarios,
when many requests are run and then collected back together. Dunno if this will ever be needed, may reconsider."
  (.findOne db
            (clj->js (:query query-req))
            (fn [err doc]
              (when err (println "db error while finding" query-req))
              (put! ch
                    {:id (:id query-req)
                     :res (js->clj doc :keywordize-keys true)
                     :err err})))
  ch)

(defn db-insert [db ch record]
  (.insert db (clj->js record) (fn [err doc]
                                 (when err (println "db error while inserting" record))
                                 (if err
                                   (put! ch {:error (str "failed to insert record " record ", error " err)})
                                   (put! ch (js->clj doc :keywordize-keys true)))))
  ch)

(defn query-req [query]
  (hash-map :id (gen-id) :query query))

(defn find-one [db ch query]
  "executes a query and returns a found entity or an empty map if not found.
Returns a channel from which a resulting entity can be read on completion"
  (go
    (let [query-res (<! (db-find-one db (chan) (query-req query)))]
      (if-let [err (:error query-res)]
        (put! ch {:error (str "failed to run query " query ": " err)})
        (put! ch (if-let [r (:res query-res)] r {})))))
  ch)

(defn resolve-entities! [db entity-coll query-keys]
  "Searches for entities in DB and inserts those missing. Each entity is searched by constructing a query using passed query keys. Returns a channel which will contain a single item - the collection with entities, all of which will have a valid db id e.g. [{:name '...' :_id '...'}]"
  (let [c (chan)
        out-ch (chan)]
    (go
      (doseq [e entity-coll]
        (let [re (<! (find-one db c (select-keys e query-keys)))]
          (when (:error re) (println (str "warning! error while searching for entity "
                                          e ", will try to insert it anyway")))
          ;; if not found, need to insert...
          (if (:_id re)
            (put! out-ch re)
            (put! out-ch (<! (db-insert db c e))))))
      (close! out-ch))
    (async/into [] out-ch)))

(defn get-distinct-artists [tracks]
  "Returns a seq of artist data, like {:name '...', ...}"
  (->> tracks
       (map (fn [t]
              (-> t
                  (select-keys [:artist])
                  (rename-keys {:artist :name})
                  (assoc :type :artist))))
       (distinct)))

(defn get-distinct-albums [tracks resolved-artists]
  "Returns a seq of album data, like {:name '...', :artist 'id', :type :album}"
  (->> tracks
      (map (fn [t]
             (-> t
                 (select-keys [:album :artist])
                 (rename-keys {:album :name})
                 (assoc :type :album))))
      (distinct)
      (map (fn [a]
             (let [artist-id (:_id (find-first #(= (:name %) (:artist a)) resolved-artists))]
               (assoc a :artist artist-id))))))

(defn resolve-track-links [t artists albums]
  (let [artist-id (:_id (find-first #(= (:name %) (:artist t))
                                    artists))
        album-id (:_id (find-first  #(and (= (:name %) (:album t) )
                                          (= (:artist %) artist-id))
                                    albums))]
    (when (nil? artist-id) (println "Warning failed to get artist id"))
    (when (nil? album-id) (println "Warning failed to get album id"))
    (assoc t
      :artist artist-id
      :album album-id)))

(defn insert-tracks! [db tracks]
  (go
    ;; (missing-from-db artists/albums will be added to db by resolve! functions)
    (let [artists (get-distinct-artists tracks)
          resolved-artists (<! (resolve-entities! db artists [:name :type]))
          albums (get-distinct-albums tracks resolved-artists)
          resolved-albums (<! (resolve-entities! db albums [:name :artist :type]))
          ]
;      (println resolved-albums)
      (println (resolve-track-links tracks resolved-artists resolved-albums))
      ))
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
