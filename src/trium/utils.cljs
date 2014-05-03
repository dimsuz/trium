(ns trium.utils)

(defn find-first [pred coll]
  (first (filter pred coll)))
