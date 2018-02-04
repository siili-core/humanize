(ns humanize.schema-error-structure-test
  (:require [clojure.test :refer :all]
            [humanize.schema :as hs]
            [schema.core :as s]
            [schema.utils :refer [named-error-explain validation-error-explain]])
  (:import [schema.utils NamedError ValidationError]))

;; So turns out Schema jams _everything_ into a single vector in
;; ExceptionInfo's :error. These tests are here just to work as a spec of
;; sorts for myself.

(s/defn one :- s/Int
  [a :- s/Int]
  a)

(s/defn incompat-ret :- s/Str
  [a :- s/Int]
  a)

(s/defn three :- s/Int
  [a :- s/Int
   b :- s/Int
   c :- s/Int]
  (+ a b c))

(defn capture
  [f]
  (try
    (f)
    (throw (AssertionError. "The wrapped function did not throw an exception"))
    (catch clojure.lang.ExceptionInfo e
      (ex-data e))))

(deftest errors-test
  (testing "wrong param type is 'NamedError' in vector"
    (let [err (-> (capture (partial one :s)) :error)]
      (is (vector? err))
      (is (instance? NamedError (first err)))))
  (testing "wrong return type is 'ValidationError' as is"
    (let [err (-> (capture (partial incompat-ret 1)) :error)]
      (is (instance? ValidationError err))))
  (testing "nil in param vector means given value is ok"
    (let [err (-> (capture (partial three 1 :2 3)) :error)]
      (is (nil? (get err 0)))
      (is (instance? NamedError (get err 1)))
      (is (nil? (get err 2))))))
