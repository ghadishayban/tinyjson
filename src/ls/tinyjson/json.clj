(ns ls.tinyjson.json
  (:require [clojure.java.io :as io])
  (:import [java.io Reader]
           [com.fasterxml.jackson.core JsonFactory]))

(defn array-reader
  ([]
   (transient []))
  ([a]
   (persistent! a))
  ([a obj]
   (conj! a obj)))

(defn map-reader
  ([]
   (transient {}))
  ([m]
   (persistent! m))
  ([m k v]
   (assoc! m (keyword k) v)))

(defn parse-reader
  [rdr]
  (let [p (.. (JsonFactory.) (createParser rdr))
        clj-parser (ls.tinyjson.JsonParser. p array-reader map-reader)]
    (.parse clj-parser)))
