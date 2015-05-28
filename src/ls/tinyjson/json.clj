(ns ls.tinyjson.json
  (:require [clojure.java.io :as io])
  (:import [java.io Reader]
           [com.fasterxml.jackson.core JsonFactory JsonGenerator]
           [java.util SimpleTimeZone Date]
           [java.text SimpleDateFormat]))

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

(defprotocol WriteJson
  (write-to [_ jg]))


(defn write-array [arr ^JsonGenerator jg]
  (.writeStartArray jg)
  (reduce (fn [jg elem]
            (write-to elem jg)
            jg)
          jg arr)
  (.writeEndArray jg))

(defn named-rep
  [n]
  (if-let [ns (namespace n)]
    (str ns "/" (name n))
    (name n)))

(defn write-map [m ^JsonGenerator jg]
  (.writeStartObject jg)
  (reduce-kv (fn [^JsonGenerator jg k v]
               (let [field (if (instance? clojure.lang.Named k) (named-rep k) k)]
                 (.writeFieldName jg ^String field))
               (write-to v jg)
               jg)
             jg m)
  (.writeEndObject jg))

(def default-date-format "yyyy-MM-dd'T'HH:mm:ss'Z'")

(extend-protocol WriteJson
  nil
  (write-to [_ ^JsonGenerator ^JsonGenerator jg]
    (.writeNull jg))
  clojure.lang.IPersistentVector
  (write-to [v ^JsonGenerator jg]
    (write-array v jg))
  clojure.lang.IPersistentMap
  (write-to [m ^JsonGenerator jg]
    (write-map m jg))
  clojure.lang.IPersistentSet
  (write-to [s ^JsonGenerator jg]
    (write-array s jg))
  clojure.lang.ISeq
  (write-to [s ^JsonGenerator jg]
    (write-array s jg))
  String
  (write-to [s ^JsonGenerator jg]
    (.writeString jg s))
  Character
  (write-to [c ^JsonGenerator jg]
    (.writeString jg (.toString c)))
  Boolean
  (write-to [b ^JsonGenerator jg]
    (.writeBoolean jg b))
 
  clojure.lang.Keyword
  (write-to [kw ^JsonGenerator jg]
    (.writeString jg ^String (named-rep kw)))
  clojure.lang.Symbol
  (write-to [sym ^JsonGenerator jg]
    (.writeString jg ^String (named-rep sym)))

  ;; Number
  
  Long
  (write-to [n ^JsonGenerator jg]
    (.writeNumber jg (long n)))
  Integer
  (write-to [n ^JsonGenerator jg]
    (.writeNumber jg (int n)))
  Double
  (write-to [d ^JsonGenerator jg]
    (.writeNumber jg d))
  Float
  (write-to [f ^JsonGenerator jg]
    (.writeNumber jg f))

  clojure.lang.Ratio
  (write-to [n ^JsonGenerator jg]
    (.writeNumber jg (double n)))
  java.math.BigInteger
  (write-to [bi ^JsonGenerator jg]
    (.writeNumber jg bi))
  java.math.BigDecimal
  (write-to [bd ^JsonGenerator jg]
    (.writeNumber jg bd))
  clojure.lang.BigInt
  (write-to [bi ^JsonGenerator jg]
    (.writeNumber jg (.toBigInteger bi)))

  java.util.Date
  (write-to [d ^JsonGenerator jg]
    (let [sdf (doto (SimpleDateFormat. default-date-format)
                (.setTimeZone (SimpleTimeZone. 0 "UTC")))]
      (.writeString jg (.format sdf d))))

  java.util.Map
  (write-to [m ^JsonGenerator jg]
    (write-map m jg))
  java.util.List
  (write-to [l ^JsonGenerator jg]
    (write-array l jg))
  java.util.Set
  (write-to [l ^JsonGenerator jg]
    (write-array l jg))
  java.util.UUID
  (write-to [uuid ^JsonGenerator jg]
    (.writeString jg (.toString uuid))))

(defn generate
  [obj os]
  (let [g (.. (JsonFactory.)
              (createGenerator ^java.io.OutputStream os))] 
    (write-to obj g)
    (.close g))
  true)
