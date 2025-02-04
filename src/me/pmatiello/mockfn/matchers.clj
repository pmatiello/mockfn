(ns me.pmatiello.mockfn.matchers
  (:require [clojure.string :as str]))

(defrecord Matcher [name function expected args-desc-fn])

(defn matches?
  "Returns whether the matcher accepts the actual value."
  [matcher actual]
  ((:function matcher) actual (:expected matcher)))

(defn description
  "Describes a matcher by returning it's name and expectations."
  [matcher]
  (let [m-name       (:name matcher)
        args-desc-fn (:args-desc-fn matcher)
        expected     (-> matcher :expected args-desc-fn)]
    (->> [m-name expected]
         (filter some?)
         (str/join " "))))

(defn make
  "Produces a new matcher instance."
  ([name function expected] (make name function expected identity))
  ([name function expected description-fn] (->Matcher name function expected description-fn)))

(defn exactly
  "Returns a matcher that expects an exact value."
  [expected]
  (make "exactly" = expected))

(defn at-least
  "Returns a matcher that expects a value greater than or equal to the argument."
  [expected]
  (make "at least" >= expected))

(defn at-most
  "Returns a matcher that expects a value less than or equal to the argument."
  [expected]
  (make "at most" <= expected))

(defn any
  "Returns a matcher that expects any value."
  []
  (make "any" (constantly true) nil))

(defn a
  "Returns a matcher that expects an instance of the provided class."
  [expected]
  (make "a" #(instance? %2 %1) expected pr-str))
