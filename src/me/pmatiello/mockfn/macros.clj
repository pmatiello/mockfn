(ns me.pmatiello.mockfn.macros
  (:require [me.pmatiello.mockfn.internal.mock :as mock]))

(defn- as-redefs
  [func->definition]
  (->> func->definition
       (map (fn [[func definition]] [func `(mock/mock ~func ~definition)]))
       (apply concat)))

(defn- func->spec
  [bindings]
  (reduce
    (fn [acc [[func & args] ret-val & expected]]
      (-> acc
          (assoc-in [func :fn] func)
          (assoc-in [func :args (into [] args)]
                    {:ret-val  ret-val
                     :calls    `(atom 0)
                     :expected (into [] expected)})))
    {} bindings))

(defmacro providing
  "Replaces functions with mocks. These mocks return preconfigured values when
  called with the expected arguments.

  (providing
    [(fn-name &args) return-value
     ...]
    test-body)

  Example:
  (providing
    [(one-fn) :result]
    (is (= :result (one-fn))))"
  [bindings & body]
  `(with-redefs ~(->> bindings (partition 2) func->spec as-redefs)
     ~@body))

(defmacro verifying
  "Replaces functions with mocks. Verifies that a calls where performed the
  expected number of times.

  (verifying
    [(fn-name &args) return-value call-count-matcher
     ...]
    test-body)

  Example:
  (verifying
    [(one-fn :argument) :result (exactly 1)]
    (is (= :result (one-fn :argument))))"
  [bindings & body]
  (let [specs# (->> bindings (partition 3) func->spec)]
    `(with-redefs ~(as-redefs specs#)
       ~@body
       (doseq [mock# (keys ~specs#)] (mock/verify mock#)))))
