(ns mockfn.internal.macros
  (:require [mockfn.mock :as mock]))

(defn- func->func-sym
  "Extracts the symbol for the function being mocked.

  When a symbol fn is passed as argument, returns fn.
  When a (var fn) is passed as argument (such as when mocking private
  functions), returns fn instead of (var fn)."
  [func]
  (if (seq? func) (last func) func))

(defn as-redefs
  [func->definition]
  (->> func->definition
       (map (fn [[func definition]] [(func->func-sym func) `(mock/mock ~func ~definition)]))
       (apply concat)))

(defn bindings->specification
  [bindings]
  (reduce
    (fn [acc [[func & args] ret-val & times-expected]]
      (-> acc
          (assoc-in [func :function] func)
          (assoc-in [func :return-values (into [] args)] ret-val)
          (assoc-in [func :times-called (into [] args)] `(atom 0))
          (assoc-in [func :times-expected (into [] args)] (into [] times-expected))))
    {} bindings))
