(ns me.pmatiello.mockfn.xtras-test
  (:require [clojure.test :refer :all]
            [me.pmatiello.mockfn.clj-test :as mfn]
            [me.pmatiello.mockfn.fixtures :as f]
            [me.pmatiello.mockfn.plain :as plain]
            [me.pmatiello.mockfn.xtras :as xtras]))

(deftest return-in-order-test
  (testing "returns sequence of values at each invocation, in order"
    (let [rio (xtras/return-in-order :a :b :c)]
      (is (= :a (rio)))
      (is (= :b (rio)))
      (is (= :c (rio)))))

  (testing "invokes functions with the ::mock/invoke-fn metadata"
    (let [rio (xtras/return-in-order identity (plain/invoke identity))]
      (is (= identity (rio :x)))
      (is (= :x (rio :x)))))

  (testing "cycles through the sequence of return values"
    (let [rio (xtras/return-in-order :a :b :c)]
      (is (= [:a :b :c :a :b :c :a :b :c :a] (repeatedly 10 rio)))))

  (testing "works within the general framework structure: plain"
    (plain/providing
      [(f/one-fn :x) (xtras/return-in-order :a :b :c (plain/invoke identity))]
      (is (= :a (f/one-fn :x)))
      (is (= :b (f/one-fn :x)))
      (is (= :c (f/one-fn :x)))
      (is (= :x (f/one-fn :x)))))

  (mfn/testing "works within the general framework structure: clj-test"
    (is (= :a (f/one-fn :x)))
    (is (= :b (f/one-fn :x)))
    (is (= :c (f/one-fn :x)))
    (is (= :x (f/one-fn :x)))
    (mfn/providing
      (f/one-fn :x) (xtras/return-in-order :a :b :c (mfn/invoke identity)))))

(mfn/deftest reify-with-test
  (let [obj (xtras/reify-with f/SomeProtocol {:m1 f/one-fn :m2 f/other-fn})]
    (mfn/testing "produces an object satisfying the given protocol"
      (is (satisfies? f/SomeProtocol obj)))

    (mfn/testing "delegates method calls to the given functions"
      (is (= :one-fn (.m1 obj)))
      (is (= :other-fn-x (.m2 obj :x)))
      (is (= :other-fn-x-y (.m2 obj :x :y)))
      (mfn/providing
        (f/one-fn obj) :one-fn
        (f/other-fn obj :x) :other-fn-x
        (f/other-fn obj :x :y) :other-fn-x-y)))

  (mfn/testing "allows incomplete method-function mappings"
    (let [obj (xtras/reify-with f/SomeProtocol {})]
      (is (satisfies? f/SomeProtocol obj)))))
