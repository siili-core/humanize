#  &#129302; `(humanize ex)` &#128522;

Library for translating errors into human readable form.

[![License](https://img.shields.io/badge/License-EPL%201.0-red.svg)](https://opensource.org/licenses/EPL-1.0) [![CircleCI](https://circleci.com/gh/siilisolutions/humanize.svg?style=svg)](https://circleci.com/gh/siilisolutions/humanize)

## Installation

Add `humanize` to your project:
[![Clojars Project](https://img.shields.io/clojars/v/siili/humanize.svg)](https://clojars.org/siili/humanize)

> Not sure if humanize is for you? We recommend testing it with [`lein try`](https://github.com/rkneufeld/lein-try)!

## Usage

Base usage is very simple:
 1. Require namespace you need
 1. Use `ex->err` function from humanize's namespace to convert exceptions into translated error data
 1. Use that data in any way you please

### Translated error data format

 - The main data structure is always a map
 - There are three keywords which describe where the error(s) occurred:
   - `:in` input to function is faulty
   - `:out` function's output is faulty
   - `:unknown` faulty data occurred in unspecified location
 - `:in` is a vector where each entry describes one argument to function in the same order as they are defined in function signature
 - `:out` is a single value
 - `:unknown` is unspecified

### Example: with [Plumatic Schema](https://github.com/plumatic/schema)

```clojure
;;  require needed namespaces
(require '[schema.core :as s]
         '[humanize.schema :as h])

;;  humanize is meant for interop with s/defn functions
(s/defn broken :- s/Str
  [x :- s/Int]
  x)

;;  boilerplate for calling the broken function in invalid way
(defn check [f]
  (try
    (f)
    (catch clojure.lang.ExceptionInfo e
      (if (= (-> e ex-data :type)
             :schema.core/error)
        (h/ex->err e))))

(check #(broken "two"))

=> {:in ([x "'two' is not an integer."])}
```

## Extending

Humanize can only handle the built-in types and structures. For user defined types an additional translator function can be provided to utilize humanize's internal resolver logic.

For example, assuming the following regular expression checking variant schema has been defined by user:
```clojure
(ns my.ns
  (:require [schema.core :as s]
            [schema.spec.variant :as variant]
            [schema.spec.core :as spec]))

(defrecord RegexString [regex]
  s/Schema
  (spec [this]
    (variant/variant-spec
     spec/+no-precondition+
     [{:schema s/Str}]
     nil
     ;; take special note of this line, the list at the end is important
     (spec/precondition this (partial re-matches regex) #(list 'not-matching regex %))))
  (explain [this]
    (list 'regex-constrained (s/explain s/Str) regex)))
```

which is then used to define a custom validator:
```clojure
(def AtoZ (RegexString. #"[AZ]+"))

(s/defn yelling-alphas :- s/Any
  [aagh :- AtoZ]
  aagh)
```
running this through humanize in same manner as above would produce unresolved translation:
```clojure
(check #(yelling-alphas "123"))

=> {:in ([aagh [not [not-matching #"[AZ]+" "123"]]])}
```
which isn't that useful. To resolve this, simply provide a additional translations function to `ex->err`:
```clojure
(defn check [f additional-translations]
  (try
    (f)
    (catch clojure.lang.ExceptionInfo e
      (if (= (-> e ex-data :type)
             :schema.core/error)
        (h/ex->err e additional-translations)))))
```
and call it with your own logic (we recommend [clojure/core.match](https://github.com/clojure/core.match)) to get the desired result:

```clojure
(defn my-translate [x]
  (clojure.core.match/match
    x
    ;; this matches with the spec/precondition list in variant spec
    ['not ['not-matching regex-pattern value]]
    (str value " does not match regex pattern " regex-pattern)

    :else
    x))

(check #(yelling-alphas "123") my-translate)
=> {:in ([aagh "123 does not match regex pattern [AZ]+"])}
```

## Acknowledgements

[André Rauh](http://arauh.net/) for the original Plumatic Schema exception unroller.

## License

Copyright © 2018 Siili Solutions

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
