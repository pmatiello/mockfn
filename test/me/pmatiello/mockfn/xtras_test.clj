(ns me.pmatiello.mockfn.xtras-test
  (:require [clojure.test :refer :all]
            [me.pmatiello.mockfn.clj-test :as mfn]
            [me.pmatiello.mockfn.fixtures :as f]
            [me.pmatiello.mockfn.internal.mock :as mock]
            [me.pmatiello.mockfn.matchers :as matchers]
            [me.pmatiello.mockfn.plain :as plain]
            [me.pmatiello.mockfn.xtras :as xtras])
  (:import (clojure.lang Compiler$CompilerException Keyword)))

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
      (is (satisfies? f/SomeProtocol obj))))

  (mfn/testing "fails at macro expansion when mapping non-protocol methods"
    (is (thrown?
          Compiler$CompilerException
          (macroexpand
            '(me.pmatiello.mockfn.xtras/reify-with
               me.pmatiello.mockfn.fixtures/SomeProtocol
               {:missing me.pmatiello.mockfn.fixtures/one-fn}))))))

(deftest calls-ordered?-test
  (testing "matches when no calls are specified"
    (binding [mock/*call-log* (atom [[#'f/one-fn :arg]
                                     [#'f/other-fn :arg1 :arg2]])]
      (is (xtras/calls-ordered?))))

  (testing "matches when a single call is specified"
    (binding [mock/*call-log* (atom [[#'f/one-fn :arg]
                                     [#'f/other-fn :arg1 :arg2]])]
      (is (xtras/calls-ordered? [#'f/one-fn :arg]))))

  (testing "does not match missing calls"
    (binding [mock/*call-log* (atom [])]
      (is (false? (xtras/calls-ordered? [#'f/one-fn :arg])))))

  (testing "matches expected calls in order"
    (binding [mock/*call-log* (atom [[#'f/one-fn :arg]
                                     [#'f/other-fn :arg1 :arg2]])]
      (is (xtras/calls-ordered?
            [#'f/one-fn :arg]
            [#'f/other-fn :arg1 :arg2]))))

  (testing "does not match calls in the wrong order"
    (binding [mock/*call-log* (atom [[#'f/one-fn :arg]
                                     [#'f/other-fn :arg]])]
      (is (false? (xtras/calls-ordered?
                    [#'f/other-fn :arg]
                    [#'f/one-fn :arg])))))

  (testing "allows unrelated calls between expected calls"
    (binding [mock/*call-log* (atom [[#'f/one-fn :arg]
                                     [#'f/other-fn :unrelated]
                                     [#'f/another-fn]])]
      (is (xtras/calls-ordered?
            [#'f/one-fn :arg]
            [#'f/another-fn]))))

  (testing "allows extra matching calls before the next expected call"
    (binding [mock/*call-log* (atom [[#'f/one-fn :arg]
                                     [#'f/one-fn :arg]
                                     [#'f/other-fn :arg1 :arg2]])]
      (is (xtras/calls-ordered?
            [#'f/one-fn :arg]
            [#'f/other-fn :arg1 :arg2]))))

  (testing "allows extra matching calls after the expected sequence"
    (binding [mock/*call-log* (atom [[#'f/one-fn :arg]
                                     [#'f/other-fn :arg1 :arg2]
                                     [#'f/one-fn :arg]])]
      (is (xtras/calls-ordered?
            [#'f/one-fn :arg]
            [#'f/other-fn :arg1 :arg2]))))

  (testing "allows in order repeated calls at different positions"
    (binding [mock/*call-log* (atom [[#'f/one-fn :arg]
                                     [#'f/other-fn :arg1 :arg2]
                                     [#'f/one-fn :arg]])]
      (is (xtras/calls-ordered?
            [#'f/one-fn :arg]
            [#'f/other-fn :arg1 :arg2]
            [#'f/one-fn :arg]))))

  (testing "distinguishes calls to different functions with the same arguments"
    (binding [mock/*call-log* (atom [[#'f/one-fn :arg]
                                     [#'f/other-fn :arg]])]
      (is (xtras/calls-ordered?
            [#'f/one-fn :arg]
            [#'f/other-fn :arg]))
      (is (false? (xtras/calls-ordered?
                    [#'f/other-fn :arg]
                    [#'f/one-fn :arg])))))

  (testing "distinguishes calls to the same function with different arguments"
    (binding [mock/*call-log* (atom [[#'f/one-fn :first]
                                     [#'f/one-fn :second]])]
      (is (xtras/calls-ordered?
            [#'f/one-fn :first]
            [#'f/one-fn :second]))
      (is (false? (xtras/calls-ordered?
                    [#'f/one-fn :second]
                    [#'f/one-fn :first])))))

  (testing "matches repeated expectations to separate calls"
    (binding [mock/*call-log* (atom [[#'f/one-fn :arg]
                                     [#'f/one-fn :arg]])]
      (is (xtras/calls-ordered?
            [#'f/one-fn :arg]
            [#'f/one-fn :arg]))))

  (testing "does not match repeated expectations to a single call"
    (binding [mock/*call-log* (atom [[#'f/one-fn :arg]])]
      (is (false? (xtras/calls-ordered?
                    [#'f/one-fn :arg]
                    [#'f/one-fn :arg])))))

  (testing "handles a missing call log as an empty call log"
    (binding [mock/*call-log* nil]
      (is (xtras/calls-ordered?))
      (is (false? (xtras/calls-ordered? [#'f/one-fn])))))

  (testing "matches and rejects calls using argument matchers"
    (binding [mock/*call-log* (atom [[#'f/one-fn :fixed 12]
                                     [#'f/other-fn :actual]])]
      (is (xtras/calls-ordered?
            [#'f/one-fn :fixed (matchers/at-least 10)]
            [#'f/other-fn (matchers/a Keyword)]))
      (is (false? (xtras/calls-ordered?
                    [#'f/one-fn :fixed (matchers/at-most 10)]
                    [#'f/other-fn (matchers/a Keyword)])))))

  (testing "skips calls that do not match an argument matcher"
    (binding [mock/*call-log* (atom [[#'f/one-fn 5]
                                     [#'f/one-fn 12]])]
      (is (xtras/calls-ordered?
            [#'f/one-fn (matchers/at-least 10)]))))

  (testing "matches and rejects calls using variadic argument matchers"
    (binding [mock/*call-log* (atom [[#'f/one-fn]
                                     [#'f/one-fn :x]
                                     [#'f/one-fn :x :y :z]])]
      (is (xtras/calls-ordered?
            [#'f/one-fn (matchers/*> (matchers/a Keyword))]
            [#'f/one-fn (matchers/*> (matchers/a Keyword))]
            [#'f/one-fn (matchers/*> (matchers/a Keyword))])))
    (binding [mock/*call-log* (atom [[#'f/one-fn :x 42]])]
      (is (false? (xtras/calls-ordered?
                    [#'f/one-fn (matchers/*> (matchers/a Keyword))])))))

  (testing "works with plain macros"
    (plain/providing
      [(f/one-fn :first) :one
       (f/other-fn :second) :other]
      (f/one-fn :first) (f/other-fn :second)
      (is (xtras/calls-ordered?
            [#'f/one-fn :first]
            [#'f/other-fn :second])))))
