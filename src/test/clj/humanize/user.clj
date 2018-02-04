(ns humanize.user
  (require [clojure.pprint :refer [pprint]]
           [schema.core :as s]
           [humanize.schema :as husch]))

(s/set-fn-validation! true)

(s/defschema map-schema {:int s/Int
                         :str s/Str
                         :bool s/Bool
                         :vec [s/Int]
                         :map {s/Any s/Any}
                         :nested {:vec [{s/Str s/Int}]
                                  :map {s/Str s/Int}
                                  :deep {:a s/Int}}})
(def broken-map {:int "1"
                 :str :string
                 :vec "vector"
                 :map {:foo :bar}
                 :nested {:vec '(:a :b)
                          :map {false true}
                          :deep {:a "hello"}}})

(s/defn foo :- s/Any
  [m :- map-schema]
  (println m))

(s/defn bar :- s/Any
  [v :- s/Int]
  (println v))

(s/defn baz :- s/Any
  [a :- s/Int
   b :- s/Int]
  (println a b))

(s/defn bam :- s/Int
  [a :- s/Int
   b :- s/Int]
  (println a b))

(defn fail
  [f]
  (try (f) (catch clojure.lang.ExceptionInfo ei ei)))

(defn fail-map
  [v]
  (fail (partial foo v)))

(defn fail-val
  [v]
  (fail (partial bar v)))

(defn fail-many
  [a b]
  (fail (partial baz a b)))

(defn fail-all
  [a b]
  (fail (partial bam a b)))
