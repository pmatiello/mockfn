(ns me.pmatiello.mockfn.plain
  (:require [me.pmatiello.mockfn.internal.mock :as mock])
  (:import (clojure.lang ExceptionInfo)))

(defn ^:private fn-sym
  [func]
  (cond
    (symbol? func) func
    (seq? func) (last func)))

(defn ^:private partition-strictly
  [n coll]
  (if (-> coll count (mod n) zero?)
    (partition n coll)
    (throw (ex-info "Malformed bindings" {:bindings coll}))))

(defn ^:private func->spec
  [bindings]
  (reduce
    (fn [acc [[func & args] ret-val & expected]]
      (let [rule {:args     (into [] args)
                  :ret-val  ret-val
                  :calls    `(atom 0)
                  :expected (into [] expected)}]
        (-> acc
            (assoc-in [func :fn] func)
            (update-in [func :rules] #(conj (or % []) rule)))))
    {} bindings))

(defn ^:private as-redefs
  [func->spec]
  (->> func->spec
       (map (fn [[func spec]] [(fn-sym func) `(mock/mock ~func ~spec)]))
       (apply concat)))

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
  `(with-redefs ~(->> bindings (partition-strictly 2) func->spec as-redefs)
     ~@body))

(defmacro verifying
  "Replaces functions with mocks. Verifies that all calls where performed the
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
  (let [specs#  (->> bindings (partition-strictly 3) func->spec)
        un-var# #(if (var? %) (var-get %) %)]
    `(with-redefs ~(as-redefs specs#)
       (let [result# (do ~@body)]
         (doseq [mock# (->> ~specs# keys (map ~un-var#))]
           (mock/verify mock#))
         result#))))

(defmacro verifying-eventually
  "Replaces functions with mocks. Verifies that all calls where performed the
  expected number of times. Performs this verification repeatedly, pausing for
  the specified interval between checks, until all expectations are met or the
  maximum number of attempts is exceeded.

  ```
  (verifying-eventually
    {:max-attempts max-attempts-num :interval-ms interval-between-attempts-ms}
    [(fn-name &args) return-value call-count-matcher
     ...]
    test-body)
  ```

  Example:
  ```
  (plain/verifying-eventually
    {:max-attempts 50 :interval-ms 20}
    [(f/one-fn) :mocked (matchers/exactly 1)]
    (future (Thread/sleep 50) (f/one-fn))))
  ```"
  [patience-cfg bindings & body]
  (let [specs#        (->> bindings (partition-strictly 3) func->spec)
        un-var#       #(if (var? %) (var-get %) %)
        max-attempts# (:max-attempts patience-cfg)
        interval-ms#  (:interval-ms patience-cfg)]
    (assert (some-> max-attempts# pos?))
    (assert (some? interval-ms#))
    `(with-redefs ~(as-redefs specs#)
       (let [result# (do ~@body)]
         (loop [attempt# 0]
           (let [vrf# (try (doseq [mock# (->> ~specs# keys (map ~un-var#))] (mock/verify mock#))
                           (catch ExceptionInfo e# e#))]
             (cond
               (and (ex-data vrf#) (>= attempt# ~max-attempts#))
               (throw vrf#)

               (ex-data vrf#)
               (do (Thread/sleep ~interval-ms#) (recur (inc attempt#)))

               :otherwise
               result#)))))))

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
