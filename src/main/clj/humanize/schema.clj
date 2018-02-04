(ns humanize.schema
  (:require [clojure.walk :refer [postwalk]]
            [clojure.core.match :refer [match]]
            [schema.utils :refer [validation-error-explain]]
            [schema.core :as s])
  (:import (schema.utils ValidationError)))

;; Originally based on and then heavily refactored from
;; https://gist.github.com/rauhs/cfdb55a8314e0d3f4862

(defn- vectorize
  "Recursively transforms all seq in m to vectors.
  Because maybe you want to use core.match with it."
  [x]
  (postwalk #(if (seq? %) (vec %) %)
            x))

(defn humanize
  "Returns a human explanation of a SINGLE error.
   This is for errors which are from USER input. It is not for programming errors.
   If the error is a map/vector then this function must be applied to each of those
   values."
  ([x] (humanize x identity))
  ([x additional-translations]
   ;; http://stackoverflow.com/questions/25189031/clojure-core-match-cant-match-on-class
   ;; TLDR: We can't match on classes (it'd be bound to that symbol)
   ;; However, match will first try to match a local binding if it exists:
   (let [String  java.lang.String
         Number  java.lang.Number
         Boolean java.lang.Boolean]
     (match
      x
       ;; Schema built-in structures:
       ['not ['pos? num]]
       (str num " is not positive but it should be.")

       ['not ['instance? Number not-num]]
       (str "'" not-num "' is not a number but it should be.")

       ['not ['instance? String not-num]]
       (str "'" not-num "' is not a string but it should be.")

       ['not ['instance? Boolean not-bool]]
       (str "'" not-bool "' is not a boolean but it should be.")

       ;; We can use core.match's :guard to apply a function check:
       ;; error by s/enum: (not (#{:x :y} :foo))
       ['not [(enum :guard set?) given]]
       (str "The value must be one of " enum " but was '" given "' instead.")

       ['not ['map? val]]
       (str "The value must be a map, but was '" val "' instead.")

       ['not ['integer? non-int]]
       (str "'" non-int "' is not an integer.")

       ['not ['sequential? non-seq]]
       (str "Value is expected to be sequential, but was '" non-seq "' instead.")

       ;; Recurse into Schema's nested NamedError:
       ['named inner name]
       (humanize inner)

       ;; Error was not generic enough to resolve on this level:
       :else
       (additional-translations x)))))

(defn- explain
  [x additional-translations]
  (cond
    (map? x)
    (into
     (sorted-map)
     (map
      (fn [[k v]]
        [(explain k additional-translations)    ;; the key itself might be a validation error
         (explain v additional-translations)])  ;; value might actually indicate issues with the key
      x))

    (or (seq? x)
        (coll? x))
    (mapv #(explain % additional-translations) x)

    (instance? ValidationError x)
    (humanize (vectorize (validation-error-explain x)) additional-translations)

    ;; these are Schema's built-in symbols which are kind of hard to match
    ;; in uniform fashion so it's easier to just do the replacement here
    (symbol? x)
    (cond
      (= 'missing-required-key x)
      "Missing required key"

      (= 'invalid-key)
      "Invalid key.")

    :else
    x))

(defn- explain-single
  "Explains single NamedError. In case the value is nil (indicates no error)
   the key :ok is instead returned in its place."
  [ne additional-translations]
  (if-not (nil? ne)
    (explain (.error ne) additional-translations)
    :ok))

(defn- extract-param-errors
  "Zips param names with their matching error explanations."
  [err additional-translations]
  (map
   vector
   (map #(.name %)                                  (:schema err))
   (map #(explain-single % additional-translations) (:error err))))

(defn- walk
  [err additional-translations]
  (cond
    (vector? (:error err))                   {:in  (extract-param-errors err additional-translations)}
    (instance? ValidationError (:error err)) {:out err}
    :else {:unknown err}))

(defn ex->err
  "Convert Schema exception into descriptive data structure showing errors.
   Returns nil if errors could not be extracted."
  ([ex] (ex->err ex identity))
  ([ex user-provided-translations]
   (some-> ex ex-data (walk user-provided-translations))))
