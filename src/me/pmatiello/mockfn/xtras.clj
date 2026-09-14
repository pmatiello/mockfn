(ns me.pmatiello.mockfn.xtras
  (:require [me.pmatiello.mockfn.internal.mock :as mock]
            [me.pmatiello.mockfn.plain :as plain]))

(defn ^:private return-in-order*
  [values & args]
  (locking values
    (let [next-val (first @values)]
      (swap! values rest)
      (if (-> next-val meta ::mock/invoke-fn)
        (apply next-val args)
        next-val))))

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

(defmacro reify-with
  "Creates an object implementing a protocol that delegates calls to protocol
  methods to predefined functions.

  As the objects produced by this macro always delegate these calls to the
  specified functions, regular mocking primitives (such as providing,
  verifying, etc.) can be configured against these given functions in order to
  specify and validate interactions against the object.

  ```
  (reify-with ProtocolName {:method-name fn-name ...})
  ```

  This macro is similar to `clojure.core/reify`. However, it's also different
  in the following aspects:
  - It only accepts protocols, not interfaces;
  - It only supports one protocol per instantiation;
  - Methods are not implemented in the body but delegated to the given
  functions instead.

  Example:
  ```
  (defprotocol SomeProtocol
    (m1 [this])
    (m2 [this x]))

  (declare fn1 fn2)
  (def obj (reify-with SomeProtocol {:m1 fn1 :m2 fn2}))

  (providing
    [(fn1 obj) :fn1
     (fn2 obj :x) :fn2]
    (is (= :fn1 (.m1 obj)))
    (is (= :fn2 (.m2 obj :x))))
  ```"
  [protocol mtd->fn]
  (let [sigs (->> protocol resolve deref :sigs
                  (filter (fn [[mtd _sig]] (-> mtd->fn keys set mtd)))
                  (mapcat (fn [[mtd sig]] (map #(vector mtd %) (:arglists sig)))))]
    `(reify ~protocol
       ~@(map
           (fn [[mtd args]]
             `(~(symbol mtd) ~args (~(mtd->fn mtd) ~@args))) sigs))))
