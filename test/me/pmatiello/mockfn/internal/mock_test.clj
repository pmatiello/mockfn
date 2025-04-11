(ns me.pmatiello.mockfn.internal.mock-test
  (:require [clojure.test :refer :all]
            [me.pmatiello.mockfn.fixtures :as f]
            [me.pmatiello.mockfn.internal.mock :as mock]
            [me.pmatiello.mockfn.matchers :as matchers])
  (:import (clojure.lang ExceptionInfo Keyword)))

(deftest mock-test
  (let [spec {:fn   'f/one-fn
              :args {[]            {:ret-val :no-args :calls (atom 0)}
                     [:arg1]       {:ret-val :one-arg :calls (atom 0)}
                     [:arg1 :arg2] {:ret-val :two-args :calls (atom 0)}
                     [:nil]        {:ret-val nil :calls (atom 0)}
                     [:fn]         {:ret-val identity :calls (atom 0)}
                     [:invoke-fn]  {:ret-val (with-meta identity {::mock/invoke-fn true}) :calls (atom 0)}}}
        mock (mock/mock f/one-fn spec)]
    (testing "returns to expected calls with configured return values"
      (is (= :no-args (mock)))
      (is (= :one-arg (mock :arg1)))
      (is (= :two-args (mock :arg1 :arg2)))
      (is (= nil (mock :nil)))
      (is (= identity (mock :fn)))
      (is (= :invoke-fn (mock :invoke-fn))))

    (testing "throws exception when called with unexpected arguments"
      (is (thrown-with-msg?
            ExceptionInfo #"Unexpected call to Unbound: #'me.pmatiello.mockfn.fixtures/one-fn with args \[:unexpected\]"
            (mock :unexpected))))))

(deftest mock-call-count-test
  (let [spec {:fn   'f/one-fn
              :args {[]            {:ret-val  :no-args
                                    :calls    (atom 0)
                                    :expected [(matchers/exactly 2)]}
                     [:arg1]       {:ret-val  :one-arg
                                    :calls    (atom 0)
                                    :expected [(matchers/exactly 1)]}
                     [:arg1 :arg2] {:ret-val  :two-args
                                    :calls    (atom 0)
                                    :expected [(matchers/exactly 0)]}}}
        mock (mock/mock f/one-fn spec)]
    (testing "counts the number of times that each call was performed"
      (mock) (mock) (mock :arg1)
      (is (= 2 (-> mock meta :args (get []) :calls deref)))
      (is (= 1 (-> mock meta :args (get [:arg1]) :calls deref)))
      (is (= 0 (-> mock meta :args (get [:arg1 :arg2]) :calls deref))))

    (testing "verifies that calls were performed the expected number of times"
      (is nil? (mock/verify mock))
      (mock :arg1 :arg2)
      (is (thrown-with-msg?
            ExceptionInfo #"Expected call \(f/one-fn :arg1 :arg2\) ｢exactly 0｣ times, received 1."
            (mock/verify mock))))))

(deftest mock-match-argument-test
  (let [match-a-kw    (matchers/a Keyword)
        match-any     (matchers/any)
        match-exact-x (matchers/exactly :x)
        spec          {:fn   'f/one-fn
                       :args {[:argument]     {:ret-val :equal
                                               :calls   (atom 0)}
                              [match-a-kw]    {:ret-val :matchers-a
                                               :calls   (atom 0)}
                              [match-any]     {:ret-val :matchers-any
                                               :calls   (atom 0)}
                              [match-exact-x] {:ret-val  :matchers-exact
                                               :calls    (atom 0)
                                               :expected [(matchers/exactly 1)]}}}
        mock          (mock/mock f/one-fn spec)]
    (testing "returns to expected calls with configured return values"
      (is (= :equal (mock :argument)))
      (is (= :matchers-a (mock :any-keyword)))
      (is (= :matchers-any (mock "anything"))))

    (testing "counts the number of times that each call was performed"
      (is (= 1 (-> mock meta :args (get [:argument]) :calls deref)))
      (is (= 1 (-> mock meta :args (get [match-a-kw]) :calls deref)))
      (is (= 1 (-> mock meta :args (get [match-any]) :calls deref))))

    (testing "verifies that calls were performed the expected number of times"
      (is (thrown-with-msg?
            ExceptionInfo #"Expected call \(f/one-fn ｢exactly :x｣\) ｢exactly 1｣ times, received 0."
            (mock/verify mock))))))
