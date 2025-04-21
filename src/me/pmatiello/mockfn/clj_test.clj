(ns me.pmatiello.mockfn.clj-test
  (:require [clojure.test :as test]
            [me.pmatiello.mockfn.plain :as plain]))

(declare providing)
(declare verifying)

(defn ^:private only?
  [symbol form]
  (-> form first resolve #{symbol}))

(def ^:private providing-only? (partial only? #'providing))
(def ^:private verifying-only? (partial only? #'verifying))

(defn ^:private with-mocking
  [body]
  (let [providing-bindings# (->> body (filter providing-only?) first rest)
        verifying-bindings# (->> body (filter verifying-only?) first rest)
        actual-body#        (->> body (remove providing-only?) (remove verifying-only?))]
    `(plain/providing [~@providing-bindings#]
       (plain/verifying [~@verifying-bindings#]
         ~@actual-body#))))

(defmacro deftest
  "Declares a test function as done by `clojure.test/deftest` with built-in
  support for mocking through (optional) `providing` and `verifying` forms.

  ```
  (deftest test-name
    ; test code
    (providing
      ; one or more entries in the form:
      ; (fn-name &args) return-value
      ...)
    (verifying
      ; one or more entries in the form:
      ; (fn-name &args) return-value call-count-matcher
      ...)
  ```

  Example:
  ```
  (deftest test-name
    (is (= :ret-val (one-fn)))
    (providing
      (one-fn) :ret-val))
  ```"
  [name & body]
  `(test/deftest ~name
     ~(with-mocking body)))

(defmacro testing
  "Declares a new testing context inside a test function as done by
  `clojure.test/testing` with built-in support for mocking through (optional)
  `providing` and `verifying` forms.

  ```
  (testing \"description\"
    ; test code
    (providing
      ; one or more entries in the form:
      ; (fn-name &args) return-value
      ...)
    (verifying
      ; one or more entries in the form:
      ; (fn-name &args) return-value call-count-matcher
      ...)
  ```

  Example:
  ```
  (deftest test-name
    (testing \"context\"
      (is (= :ret-val (one-fn)))
      (verifying
        (one-fn) :ret-val (mockfn.matchers/exactly 1))))
  ```"
  [string & body]
  `(test/testing ~string
     ~(with-mocking body)))

(defn invoke
  "Marks a function to be dynamically invoked on mock calls. Matching calls
  will invoke the function with the received arguments and return the output.

  Example:
  ```
  (deftest test-name
    (is (= :invoke-fn (one-fn :invoke-fn)))
    (providing
      (one-fn :invoke-fn) (invoke identity)))
  ```"
  [func]
  (plain/invoke func))

(defn raise
  "Creates a mock behavior that throws the given exception when the mock is called.

  This can be used to simulate error scenarios in tests by configuring a mock
  to throw a specific exception.

  Example:
  ```
  (deftest raise-test
    (is (thrown? ExceptionInfo (f/one-fn)))
    (providing
      (f/one-fn) (raise (ex-info \"error!\" {}))))
  ```"
  [exception]
  (plain/raise exception))
