(ns me.pmatiello.mockfn.matchers
  (:require [clojure.string :as str]))

(defrecord Matcher [name match-fn expected args-desc-fn])

(defn matches?
  "Returns whether the matcher accepts the actual value."
  [matcher actual]
  ((:match-fn matcher) actual (:expected matcher)))

(defn description
  "Describes a matcher by returning it's name and expectations."
  [matcher]
  (let [m-name       (:name matcher)
        args-desc-fn (:args-desc-fn matcher)
        expected     (-> matcher :expected args-desc-fn)]
    (->> [m-name expected]
         (filter some?)
         (str/join " ")
         (format "｢%s｣"))))

(defn make
  "Produces a new matcher instance."
  ([name match-fn expected] (make name match-fn expected identity))
  ([name match-fn expected description-fn] (->Matcher name match-fn expected description-fn)))

(defn exactly
  "Returns a matcher that expects an exact value."
  [expected]
  (make "exactly" = expected))

(defn at-least
  "Returns a matcher that expects a value greater than or equal to the argument."
  [expected]
  (make "at-least" >= expected))

(defn at-most
  "Returns a matcher that expects a value less than or equal to the argument."
  [expected]
  (make "at-most" <= expected))

(defn any
  "Returns a matcher that expects any value."
  []
  (make "any" (constantly true) nil))

(defn a
  "Returns a matcher that expects an instance of the provided class."
  [expected]
  (make "a" #(instance? %2 %1) expected pr-str))

(defn str-starts-with
  "Returns a matcher that expects a string starting with the provided substring."
  [expected]
  (make "str-starts-with" #(str/starts-with? %1 %2) expected pr-str))

(defn str-ends-with
  "Returns a matcher that expects a string ending with the provided substring."
  [expected]
  (make "str-ends-with" #(str/ends-with? %1 %2) expected pr-str))

(defn str-includes
  "Returns a matcher that expects a string containing the provided substring."
  [expected]
  (make "str-includes" #(str/includes? %1 %2) expected pr-str))

(defn str-rexp
  "Returns a matcher that expects a string matching the provided regular expression."
  [expected]
  (make "str-rexp" #(some? (re-matches %2 %1)) expected pr-str))
