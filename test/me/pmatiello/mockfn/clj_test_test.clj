(ns me.pmatiello.mockfn.clj-test-test
  (:require [clojure.test :refer :all]
            [me.pmatiello.mockfn.clj-test :as mfn]
            [me.pmatiello.mockfn.fixtures :as f]
            [me.pmatiello.mockfn.matchers :as mfn.m])
  (:import (clojure.lang ExceptionInfo)))

(def tests-run (atom #{}))

(mfn/deftest deftest-test
  (swap! tests-run conj :deftest))

(mfn/deftest deftest-providing-test
  (swap! tests-run conj (f/one-fn))
  (mfn/providing
    (f/one-fn) :deftest-providing))

(mfn/deftest deftest-verifying-test
  (swap! tests-run conj (f/one-fn))
  (mfn/verifying
    (f/one-fn) :deftest-verifying (mfn.m/exactly 1)))

(mfn/deftest deftest-providing-and-verifying-test
  (swap! tests-run conj (f/one-fn))
  (swap! tests-run conj (f/other-fn))
  (mfn/providing
    (f/one-fn) :deftest-providing-with-verifying)
  (mfn/verifying
    (f/other-fn) :deftest-verifying-with-providing (mfn.m/exactly 1)))

(mfn/deftest private-fn-providing-test
  (swap! tests-run conj (#'f/pvt-fn))
  (mfn/providing
    (#'f/pvt-fn) :private-fn-providing))

(mfn/deftest private-fn-test
  (swap! tests-run conj (#'f/pvt-fn))
  (mfn/verifying
    (#'f/pvt-fn) :private-fn-verifying (mfn.m/exactly 1)))

(mfn/deftest testing-test
  (mfn/testing "testing"
    (swap! tests-run conj :testing)))

(mfn/deftest testing-providing-test
  (mfn/testing "testing-providing"
    (swap! tests-run conj (f/one-fn))
    (mfn/providing
      (f/one-fn) :testing-providing)))

(mfn/deftest testing-verifying-test
  (mfn/testing "testing-verifying"
    (swap! tests-run conj (f/one-fn))
    (mfn/verifying
      (f/one-fn) :testing-verifying (mfn.m/exactly 1))))

(mfn/deftest testing-providing-and-verifying-test
  (mfn/testing "testing-providing-and-verifying"
    (swap! tests-run conj (f/one-fn))
    (swap! tests-run conj (f/other-fn))
    (mfn/providing
      (f/one-fn) :testing-providing-with-verifying)
    (mfn/verifying
      (f/other-fn) :testing-verifying-with-providing (mfn.m/exactly 1))))

(mfn/deftest deftest-testing-test
  (mfn/testing "deftest-testing"
    (swap! tests-run conj (f/one-fn))
    (swap! tests-run conj (f/other-fn))
    (mfn/providing
      (f/one-fn) :deftest-testing-pt1))
  (mfn/providing
    (f/other-fn) :deftest-testing-pt2))

(mfn/deftest invoke-fn-test
  (swap! tests-run conj (f/one-fn :invoke-fn))
  (mfn/providing
    (f/one-fn :invoke-fn) (mfn/invoke identity)))

(mfn/deftest raise-test
  (is (thrown? ExceptionInfo (f/one-fn)))
  (swap! tests-run conj :raise)
  (mfn/providing
    (f/one-fn) (mfn/raise (ex-info "error!" {}))))

(def expected-tests-run
  #{:deftest
    :deftest-providing
    :deftest-providing-with-verifying
    :deftest-testing-pt1
    :deftest-testing-pt2
    :deftest-verifying
    :deftest-verifying-with-providing
    :invoke-fn
    :private-fn-providing
    :private-fn-verifying
    :raise
    :testing
    :testing-providing
    :testing-providing-with-verifying
    :testing-verifying
    :testing-verifying-with-providing})

(defn teardown []
  (is (= @tests-run expected-tests-run))
  (reset! tests-run #{}))

(defn once-fixture [f]
  (f) (teardown))

(use-fixtures :once once-fixture)
