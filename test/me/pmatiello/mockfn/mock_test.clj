(ns me.pmatiello.mockfn.mock-test
  (:require [clojure.test :refer :all]
            [me.pmatiello.mockfn.matchers :as matchers]
            [me.pmatiello.mockfn.mock :as mock])
  (:import (clojure.lang ExceptionInfo Keyword)))

(def one-fn)

(deftest mock-test
  (let [definition {:return-values {[]            :no-args
                                    [:arg1]       :one-arg
                                    [:arg1 :arg2] :two-args
                                    [:nil]        nil}
                    :times-called  {[]            (atom 0)
                                    [:arg1]       (atom 0)
                                    [:arg1 :arg2] (atom 0)
                                    [:nil]        (atom 0)}}
        mock       (mock/mock one-fn definition)]
    (testing "returns to expected calls with configured return values"
      (is (= :no-args (mock)))
      (is (= :one-arg (mock :arg1)))
      (is (= :two-args (mock :arg1 :arg2)))
      (is (= nil (mock :nil))))

    (testing "throws exception when called with unexpected arguments"
      (is (thrown-with-msg?
            ExceptionInfo #"Unexpected call to Unbound: #'me.pmatiello.mockfn.mock-test/one-fn with args \[:unexpected\]"
            (mock :unexpected))))))

(deftest mock-call-count-test
  (let [definition {:function       'one-fn
                    :return-values  {[]            :no-args
                                     [:arg1]       :one-arg
                                     [:arg1 :arg2] :two-args}
                    :times-called   {[]            (atom 0)
                                     [:arg1]       (atom 0)
                                     [:arg1 :arg2] (atom 0)}
                    :times-expected {[]            [(matchers/exactly 2)]
                                     [:arg1]       [(matchers/exactly 1)]
                                     [:arg1 :arg2] [(matchers/exactly 0)]}}
        mock       (mock/mock one-fn definition)]
    (testing "counts the number of times that each call was performed"
      (mock) (mock) (mock :arg1)
      (is (= 2 (-> mock meta (get-in [:times-called []]) deref)))
      (is (= 1 (-> mock meta (get-in [:times-called [:arg1]]) deref)))
      (is (= 0 (-> mock meta (get-in [:times-called [:arg1 :arg2]]) deref))))

    (testing "verifies that calls were performed the expected number of times"
      (is nil? (mock/verify mock))
      (mock :arg1 :arg2)
      (is (thrown-with-msg?
            ExceptionInfo #"Expected one-fn with arguments \[:arg1 :arg2\] exactly 0 times, received 1."
            (mock/verify mock))))))

(deftest mock-match-argument-test
  (let [definition {:function      'one-fn
                    :return-values {[:argument]            :equal
                                    [(matchers/a Keyword)] :matchers-a
                                    [(matchers/any)]       :matchers-any}
                    :times-called  {[:argument]            (atom 0)
                                    [(matchers/a Keyword)] (atom 0)
                                    [(matchers/any)]       (atom 0)}}
        mock       (mock/mock one-fn definition)]
    (testing "returns to expected calls with configured return values"
      (is (= :equal (mock :argument)))
      (is (= :matchers-a (mock :any-keyword)))
      (is (= :matchers-any (mock "anything"))))

    (testing "counts the number of times that each call was performed"
      (is (= 1 (-> mock meta (get-in [:times-called [:argument]]) deref)))
      (is (= 1 (-> mock meta (get-in [:times-called [(matchers/a Keyword)]]) deref)))
      (is (= 1 (-> mock meta (get-in [:times-called [(matchers/any)]]) deref))))))
