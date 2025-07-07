(ns me.pmatiello.mockfn.xtras
  (:require [me.pmatiello.mockfn.internal.mock :as mock]
            [me.pmatiello.mockfn.plain :as plain]))

(defn ^:private return-in-order*
  [values & args]
  (let [next-val (first @values)]
    (swap! values rest)
    (if (-> next-val meta ::mock/invoke-fn)
      (apply next-val args)
      next-val)))

(defn return-in-order
  "Returns a function that, when called, returns the next value from `values`
   on each invocation, cycling through them.

   If the next value is a function produced by
   - [[me.pmatiello.mockfn.plain/invoke]], or
   - [[me.pmatiello.mockfn.clj-test/invoke]],
   then the returned value will be the result of the invocation of this
   function with the received call arguments.

   Example:
   ```
   (providing
     [(one-fn :x) (return-in-order :a :b :c (invoke identity))]
     (is (= :a (one-fn :x)))
     (is (= :b (one-fn :x)))
     (is (= :c (one-fn :x)))
     (is (= :x (one-fn :x))))
   ```"
  [& values]
  (plain/invoke (partial return-in-order* (atom (cycle values)))))
