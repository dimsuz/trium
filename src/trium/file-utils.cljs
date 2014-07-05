(ns trium.file-utils)

(def fs (js/require "fs"))

(defn delete [path]
  (.unlinkSync fs path))

(defn exists [path]
  (.existsSync fs path))

(defn open [path flags]
  (.open fs path flags))
