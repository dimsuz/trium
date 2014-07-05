(ns trium.test.storage
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test run-tests testing test-var use-fixtures)])
  (:require
   [cemerick.cljs.test :as t]
   [trium.storage :as storage]
   [trium.file-utils :as fs]))

(def TEST_DB_FILE "test.db")

(defn setup-db-fixture [f]
  (f)
  (fs/delete TEST_DB_FILE))

(use-fixtures :each setup-db-fixture)
(deftest create-db
  (let [db (storage/create-db "test.db")]
    (is (not (nil? db)))
    (is (fs/exists TEST_DB_FILE))
    (is (= "sqlite3.Database" (type db)))))

;(t/test-ns 'trium.test.storage)
