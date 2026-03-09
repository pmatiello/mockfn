(ns me.pmatiello.mockfn.internal.mock-test
  (:require [clojure.test :refer :all]
            [me.pmatiello.mockfn.fixtures :as f]
            [me.pmatiello.mockfn.internal.mock :as mock]
            [me.pmatiello.mockfn.matchers :as matchers])
  (:import (clojure.lang ExceptionInfo Keyword)))

(defn- rule-for
  [mock args]
  (some->> mock meta :rules (filter #(-> % :args (= args))) first))

(deftest mock-test
  (let [spec  {:fn    'f/one-fn
               :rules [{:args [] :ret-val :no-args :calls (atom 0)}
                       {:args [:arg1] :ret-val :one-arg :calls (atom 0)}
                       {:args [:arg1 :arg2] :ret-val :two-args :calls (atom 0)}
                       {:args [:nil] :ret-val nil :calls (atom 0)}
                       {:args [:fn] :ret-val identity :calls (atom 0)}
                       {:args [:invoke-fn] :ret-val (with-meta identity {::mock/invoke-fn true}) :calls (atom 0)}]}
        mock  (mock/mock f/one-fn spec)
        spec2 {:fn    'f/other-fn
               :rules [{:args [:arg] :ret-val :one-arg :calls (atom 0)}]}
        mock2 (mock/mock f/other-fn spec2)]
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
            (mock :unexpected)))
      (is (thrown-with-msg?
            ExceptionInfo #"Unexpected call to Unbound: #'me.pmatiello.mockfn.fixtures/other-fn with args \[\]"
            (mock2)))
      (is (thrown-with-msg?
            ExceptionInfo #"Unexpected call to Unbound: #'me.pmatiello.mockfn.fixtures/other-fn with args \[:arg :unexpected\]"
            (mock2 :arg :unexpected))))))

(deftest mock-call-count-test
  (let [spec {:fn    'f/one-fn
              :rules [{:args     []
                       :ret-val  :no-args
                       :calls    (atom 0)
                       :expected [(matchers/exactly 2)]}
                      {:args     [:arg1]
                       :ret-val  :one-arg
                       :calls    (atom 0)
                       :expected [(matchers/exactly 1)]}
                      {:args     [:arg1 :arg2]
                       :ret-val  :two-args
                       :calls    (atom 0)
                       :expected [(matchers/exactly 0)]}]}
        mock (mock/mock f/one-fn spec)]
    (testing "counts the number of times that each call was performed"
      (mock) (mock) (mock :arg1)
      (is (= 2 (-> (rule-for mock []) :calls deref)))
      (is (= 1 (-> (rule-for mock [:arg1]) :calls deref)))
      (is (= 0 (-> (rule-for mock [:arg1 :arg2]) :calls deref))))

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
        spec          {:fn    'f/one-fn
                       :rules [{:args    [:argument]
                                :ret-val :equal
                                :calls   (atom 0)}
                               {:args    [match-a-kw]
                                :ret-val :matchers-a
                                :calls   (atom 0)}
                               {:args    [match-any]
                                :ret-val :matchers-any
                                :calls   (atom 0)}
                               {:args     [match-exact-x]
                                :ret-val  :matchers-exact
                                :calls    (atom 0)
                                :expected [(matchers/exactly 1)]}]}
        mock          (mock/mock f/one-fn spec)]
    (testing "returns to expected calls with configured return values"
      (is (= :equal (mock :argument)))
      (is (= :matchers-a (mock :any-keyword)))
      (is (= :matchers-any (mock "anything"))))

    (testing "counts the number of times that each call was performed"
      (is (= 1 (-> (rule-for mock [:argument]) :calls deref)))
      (is (= 1 (-> (rule-for mock [match-a-kw]) :calls deref)))
      (is (= 1 (-> (rule-for mock [match-any]) :calls deref))))

    (testing "verifies that calls were performed the expected number of times"
      (is (thrown-with-msg?
            ExceptionInfo #"Expected call \(f/one-fn ｢exactly :x｣\) ｢exactly 1｣ times, received 0."
            (mock/verify mock))))))

(deftest mock-variadic-matcher-test
  (let [match-x* (matchers/*> (matchers/exactly :x))
        spec     {:fn    'f/one-fn
                  :rules [{:args [match-x*] :ret-val :ok :calls (atom 0)}]}
        mock     (mock/mock f/one-fn spec)]
    (testing "matches any number of arguments with variadic matcher"
      (is (= :ok (mock)))
      (is (= :ok (mock :x)))
      (is (= :ok (mock :x :x :x))))
    (testing "counts calls for variadic matcher"
      (is (= 3 (-> (rule-for mock [match-x*]) :calls deref))))
    (testing "throws exception when called with unexpected arguments"
      (is (thrown? ExceptionInfo (mock :x :y :z))))))
