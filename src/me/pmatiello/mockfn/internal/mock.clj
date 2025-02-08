(ns me.pmatiello.mockfn.internal.mock
  (:require [me.pmatiello.mockfn.matchers :as matchers])
  (:import (me.pmatiello.mockfn.matchers Matcher)))

(defn- matches-arg?
  [[expected arg]]
  (if (instance? Matcher expected)
    (matchers/matches? expected arg)
    (= expected arg)))

(defn- matches-args?
  [expected args]
  (let [arity-matches?    (= (count expected) (count args))
        each-arg-matches? (every? matches-arg? (map vector expected args))]
    (and arity-matches? each-arg-matches? expected)))

(defn- for-args
  [m args]
  (let [expected (some #(matches-args? % args) (keys m))]
    (if expected
      (get m expected)
      ::unexpected-call)))

(defn- unexpected-call [func args]
  (format "Unexpected call to %s with args %s." func args))

(defn- return-value-for
  [func spec args]
  (let [spec* (-> spec :args (for-args args))]
    (when (= spec* ::unexpected-call)
      (throw (ex-info (unexpected-call func args) {})))
    (-> spec* :calls (swap! inc))
    (:ret-val spec*)))

(defn mock [func spec]
  (with-meta
    (fn [& args] (return-value-for func spec (into [] args)))
    spec))

(defn- doesnt-match [function args matcher times-called]
  (format "Expected %s with arguments %s %s times, received %s."
          function args (matchers/description matcher) times-called))

(defn verify [mock]
  (doseq [args     (-> mock meta :args keys)
          expected (-> mock meta :args (get args) :expected)]
    (let [calls (-> mock meta :args (get args) :calls deref)]
      (when-not (matchers/matches? expected calls)
        (throw (ex-info (doesnt-match (-> mock meta :fn) args expected calls) {}))))))
