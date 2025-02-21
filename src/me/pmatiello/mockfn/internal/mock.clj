(ns me.pmatiello.mockfn.internal.mock
  (:require [clojure.string :as str]
            [me.pmatiello.mockfn.matchers :as matchers])
  (:import (me.pmatiello.mockfn.matchers Matcher)))

(defn ^:private matches-arg?
  [[expected arg]]
  (if (instance? Matcher expected)
    (matchers/matches? expected arg)
    (= expected arg)))

(defn ^:private matches-args?
  [expected args]
  (let [arity-matches?    (= (count expected) (count args))
        each-arg-matches? (every? matches-arg? (map vector expected args))]
    (and arity-matches? each-arg-matches? expected)))

(defn ^:private for-args
  [m args]
  (let [expected (some #(matches-args? % args) (keys m))]
    (if expected
      (get m expected)
      ::unexpected-call)))

(defn ^:private unexpected-call [func args]
  (format "Unexpected call to %s with args %s." func args))

(defn ^:private return-value-for
  [func spec args]
  (let [spec* (-> spec :args (for-args args))]
    (when (= spec* ::unexpected-call)
      (throw (ex-info (unexpected-call func args) {})))
    (-> spec* :calls (swap! inc))
    (:ret-val spec*)))

(defn mock [func spec]
  "Creates a mock function that returns predefined values when called with
  expected arguments."
  (with-meta
    (fn [& args] (return-value-for func spec (into [] args)))
    spec))

(defn ^:private arg->str [arg]
  (cond
    (instance? Matcher arg) (matchers/description arg)
    :else (pr-str arg)))

(defn ^:private call->str [function args]
  (->> (cons function args)
       (mapv arg->str)
       (str/join " ")))

(defn ^:private doesnt-match [function args matcher times-called]
  (format "Expected call (%s) %s times, received %s."
          (call->str function args) (matchers/description matcher) times-called))

(defn verify [mock]
  "Verifies that the given mock function was called the expected number of
  times with the expected arguments."
  (doseq [args     (-> mock meta :args keys)
          expected (-> mock meta :args (get args) :expected)]
    (let [times-called (-> mock meta :args (get args) :calls deref)]
      (when-not (matchers/matches? expected times-called)
        (throw (ex-info (doesnt-match (-> mock meta :fn) args expected times-called) {}))))))
