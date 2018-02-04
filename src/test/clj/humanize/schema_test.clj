(ns humanize.schema-test
  (:require [clojure.test :refer :all]
            [humanize.schema :as hs]
            [schema.core :as s]
            [schema.utils :refer [named-error-explain validation-error-explain]])
  (:import [schema.utils NamedError ValidationError]))

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

(defn inspect
  "Simply captures exceptions thrown by given function and returns the
  exception for further inspection."
  [f]
  (try
    (f)
    (throw (AssertionError. "The wrapped function did not throw an exception"))
    (catch clojure.lang.ExceptionInfo e
      e)))

(deftest errors-test
  (testing "single param error can be humanized"
    (let [err (-> (inspect (partial one :s)) hs/ex->err)]
      (is (= "':s' is not an integer."
             (hs/humanize (-> err :in first second))))))

  (comment
    "TODO: This got broken somewhere along the way, we shall fix 'em soon"
    (testing "return type error can be humanized"
    (let [err (-> (inspect (partial incompat-ret 1)) hs/ex->err)]
      (println err)
      (is (= "'1' is not a string but it should be."
             (hs/humanize (-> err :out :error))))))))
