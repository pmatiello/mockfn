(ns me.pmatiello.mockfn.matchers
  (:require [clojure.string :as str]))

(defrecord Matcher [name function expected])

(defn matches? [matcher actual]
  ((:function matcher) actual (:expected matcher)))

(defn description [matcher]
  (->> [(:name matcher) (:expected matcher)]
       (filter some?)
       (str/join " ")))

(defn exactly [expected]
  (->Matcher "exactly" = expected))

(defn at-least [expected]
  (->Matcher "at least" >= expected))

(defn at-most [expected]
  (->Matcher "at most" <= expected))

(defn any []
  (->Matcher "any" (constantly true) nil))

(defn a [expected]
  (->Matcher "a" #(instance? %2 %1) expected))
