(ns me.pmatiello.mockfn.matchers
  (:refer-clojure :exclude [empty some])
  (:require [clojure.set :as set]
            [clojure.string :as str]))

(defrecord Matcher [name match-fn expected args-desc-fn expand-fn])

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

(defn expand
  "Expands a matcher using its expand-fn."
  [matcher args]
  ((:expand-fn matcher) matcher args))

(defn make
  "Produces a new matcher instance."
  ([name match-fn expected]
   (make name match-fn expected identity))
  ([name match-fn expected args-desc-fn]
   (make name match-fn expected args-desc-fn (fn [m _a] [m])))
  ([name match-fn expected args-desc-fn expand-fn]
   (->Matcher name match-fn expected args-desc-fn expand-fn)))

(defn any
  "Returns a matcher that expects any value."
  []
  (make "any" (constantly true) nil))

(defn a
  "Returns a matcher that expects an instance of the provided type."
  [type]
  (make "a" #(instance? %2 %1) type pr-str))

(defn exactly
  "Returns a matcher that expects an exact value."
  [value]
  (make "exactly" = value))

(defn pred
  "Returns a matcher that expects a value satisfying the provided predicate."
  [pred-fn]
  (make "pred" #(%2 %1) pred-fn #(-> % class pr-str)))

(defn some
  "Returns a matcher that expects a non-nil value."
  []
  (make "some" (fn [a _] (some? a)) nil))

(defn null
  "Returns a matcher that expects a nil value."
  []
  (make "null" (fn [a _] (nil? a)) nil))

(defn empty
  "Returns a matcher that expects an empty value."
  []
  (make "empty" (fn [a _] (and (seqable? a) (empty? a))) nil))

(defn truthy
  "Returns a matcher that expects a truthy value."
  []
  (make "truthy" (fn [a _] (boolean a)) nil))

(defn falsy
  "Returns a matcher that expects a falsy value."
  []
  (make "falsy" (fn [a _] (not (boolean a))) nil))

(defn at-least
  "Returns a matcher that expects a value greater than or equal to the argument."
  [value]
  (make "at-least" (fn [a e] (and (number? a) (number? e) (>= a e))) value))

(defn at-most
  "Returns a matcher that expects a value less than or equal to the argument."
  [value]
  (make "at-most" (fn [a e] (and (number? a) (number? e) (<= a e))) value))

(defn between
  "Returns a matcher that expects a value between lower-bound and upper-bound, inclusive."
  [lower-bound upper-bound]
  (make "between" (fn [a [lo up]]
                    (and (number? a)
                         (number? lo)
                         (number? up)
                         (>= a lo)
                         (<= a up)))
        [lower-bound upper-bound] #(str/join " and " %)))

(defn starts-with
  "Returns a matcher that expects a string starting with the provided prefix."
  [prefix]
  (make "starts-with" #(and (string? %1) (string? %2) (str/starts-with? %1 %2)) prefix pr-str))

(defn ends-with
  "Returns a matcher that expects a string ending with the provided suffix."
  [suffix]
  (make "ends-with" #(and (string? %1) (string? %2) (str/ends-with? %1 %2)) suffix pr-str))

(defn includes
  "Returns a matcher that expects a string containing the provided substring."
  [substring]
  (make "includes" #(and (string? %1) (string? %2) (str/includes? %1 %2)) substring pr-str))

(defn regex
  "Returns a matcher that expects a string matching the provided regular expression."
  [regex]
  (make "regex" #(and (instance? java.util.regex.Pattern %2)
                      (string? %1)
                      (some? (re-matches %2 %1)))
        regex pr-str))

(defn contains-all
  "Returns a matcher that expects a collection containing all the provided values."
  [values]
  (make "contains-all"
        #(and (coll? %1) (-> %1 set (set/intersection %2) (= %2)))
        (set values)
        pr-str))

(defn contains-any
  "Returns a matcher that expects a collection containing at least one of the provided values."
  [values]
  (make "contains-any"
        #(and (coll? %1) (-> %1 set (set/intersection %2) empty? not))
        (set values)
        pr-str))

(defn not>
  "Returns a matcher that expects a value not matching the provided matcher."
  [matcher]
  (make "not>" #(not (matches? %2 %1)) matcher description))

(defn ^:private description*
  [matchers]
  (->> matchers (map description) (str/join " ")))

(defn and>
  "Returns a matcher that expects a value matching all provided matchers."
  [& matchers]
  (make "and>" (fn [a e] (every? #(matches? % a) e)) matchers description*))

(defn or>
  "Returns a matcher that expects a value matching any of the provided matchers."
  [& matchers]
  (make "or>" (fn [a e] (boolean (clojure.core/some #(matches? % a) e))) matchers description*))

(defn *>
  "Returns a matcher that matches a sequence of arguments against the provided matcher."
  [matcher]
  (make "*>" (fn [_ _] (throw (ex-info "Invalid operation." {}))) matcher description
        (fn [ma args] (repeat (count args) (:expected ma)))))
