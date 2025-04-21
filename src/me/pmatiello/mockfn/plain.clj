(ns me.pmatiello.mockfn.plain
  (:require [me.pmatiello.mockfn.internal.mock :as mock]))

(defn ^:private fn-sym
  [func]
  (cond
    (symbol? func) func
    (seq? func) (last func)))

(defn ^:private as-redefs
  [func->definition]
  (->> func->definition
       (map (fn [[func definition]] [(fn-sym func) `(mock/mock ~func ~definition)]))
       (apply concat)))

(defn ^:private func->spec
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

  ```
  (providing
    [(fn-name &args) return-value
     ...]
    test-body)
  ```

  Example:
  ```
  (providing
    [(one-fn) :result]
    (is (= :result (one-fn))))
  ```"
  [bindings & body]
  `(with-redefs ~(->> bindings (partition 2) func->spec as-redefs)
     ~@body))

(defmacro verifying
  "Replaces functions with mocks. Verifies that a calls where performed the
  expected number of times.

  ```
  (verifying
    [(fn-name &args) return-value call-count-matcher
     ...]
    test-body)
  ```

  Example:
  ```
  (verifying
    [(one-fn :argument) :result (exactly 1)]
    (is (= :result (one-fn :argument))))
  ```"
  [bindings & body]
  (let [specs#  (->> bindings (partition 3) func->spec)
        un-var# #(if (var? %) (var-get %) %)]
    `(with-redefs ~(as-redefs specs#)
       (let [result# (do ~@body)]
         (doseq [mock# (->> ~specs# keys (map ~un-var#))]
           (mock/verify mock#))
         result#))))

(defn invoke
  "Marks a function to be dynamically invoked on mock calls. Matching calls
  will invoke the function with the received arguments and return the output.

  Example:
  ```
  (providing
    [(one-fn :invoke-fn) (invoke identity)]
    (is (= :invoke-fn (one-fn :invoke-fn))))
  ```"
  [func]
  (with-meta func {::mock/invoke-fn true}))

(defn raise
  "Creates a mock behavior that throws the given exception when the mock is called.

  This can be used to simulate error scenarios in tests by configuring a mock
  to throw a specific exception.

  Example:
  ```
  (providing
    [(one-fn) (raise (ex-info \"error!\" {}))]
    (is (thrown-with-msg? ExceptionInfo #\"error!\" (one-fn))))
  ```"
  [exception]
  (invoke (fn [& _] (throw exception))))
