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

(mfn/deftest deftest-verifying-eventually-test
  (future (Thread/sleep 50) (swap! tests-run conj (f/one-fn)))
  (mfn/verifying-eventually
    {:max-attempts 50 :interval-ms 20}
    (f/one-fn) :deftest-verifying-eventually (mfn.m/exactly 1)))

(mfn/deftest deftest-providing+verifying-test
  (swap! tests-run conj (f/one-fn))
  (swap! tests-run conj (f/other-fn))
  (mfn/providing
    (f/one-fn) :deftest-providing+verifying-pt1)
  (mfn/verifying
    (f/other-fn) :deftest-providing+verifying-pt2 (mfn.m/exactly 1)))

(mfn/deftest deftest-verifying-eventually-test
  (future (Thread/sleep 50) (swap! tests-run conj (f/one-fn)))
  (mfn/verifying-eventually
    {:max-attempts 50 :interval-ms 20}
    (f/one-fn) :deftest-verifying-eventually (mfn.m/exactly 1)))

(mfn/deftest deftest-providing+verifying+verifying-eventually-test
  (swap! tests-run conj (f/one-fn))
  (swap! tests-run conj (f/other-fn))
  (future (Thread/sleep 50) (swap! tests-run conj (f/another-fn)))
  (mfn/providing
    (f/one-fn) :deftest-providing+verifying+verifying-eventually-pt1)
  (mfn/verifying
    (f/other-fn) :deftest-providing+verifying+verifying-eventually-pt2 (mfn.m/exactly 1))
  (mfn/verifying-eventually
    {:max-attempts 50 :interval-ms 20}
    (f/another-fn) :deftest-providing+verifying+verifying-eventually-pt3 (mfn.m/exactly 1)))

(mfn/deftest private-fn-providing-test
  (swap! tests-run conj (#'f/pvt-fn))
  (mfn/providing
    (#'f/pvt-fn) :private-fn-providing))

(mfn/deftest private-fn-verifying-test
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

(mfn/deftest testing-verifying-eventually-test
  (mfn/testing "testing-verifying-eventually"
    (future (Thread/sleep 50) (swap! tests-run conj (f/one-fn)))
    (mfn/verifying-eventually
      {:max-attempts 50 :interval-ms 20}
      (f/one-fn) :testing-verifying-eventually (mfn.m/exactly 1))))

(mfn/deftest testing-providing+verifying-test
  (mfn/testing "testing-providing-and-verifying"
    (swap! tests-run conj (f/one-fn))
    (swap! tests-run conj (f/other-fn))
    (mfn/providing
      (f/one-fn) :testing-providing+verifying-pt1)
    (mfn/verifying
      (f/other-fn) :testing-providing+verifying-pt2 (mfn.m/exactly 1))))

(mfn/deftest testing-providing+verifying+verifying-eventually-test
  (mfn/testing "testing-providing-and-verifying"
    (swap! tests-run conj (f/one-fn))
    (swap! tests-run conj (f/other-fn))
    (future (Thread/sleep 50) (swap! tests-run conj (f/another-fn)))
    (mfn/providing
      (f/one-fn) :testing-providing+verifying+verifying-eventually-pt1)
    (mfn/verifying
      (f/other-fn) :testing-providing+verifying+verifying-eventually-pt2 (mfn.m/exactly 1))
    (mfn/verifying-eventually
      {:max-attempts 50 :interval-ms 20}
      (f/another-fn) :testing-providing+verifying+verifying-eventually-pt3 (mfn.m/exactly 1))))

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

(deftest multiple-providing-forms-test
  (mfn/testing "testing-multiple-providing-forms"
    (swap! tests-run conj (f/one-fn))
    (swap! tests-run conj (f/other-fn))
    (mfn/providing
      (f/one-fn) :multiple-providing-forms-pt1)
    (mfn/providing
      (f/other-fn) :multiple-providing-forms-pt2)))

(deftest multiple-verifying-forms-test
  (mfn/testing "testing-multiple-providing-forms"
    (swap! tests-run conj (f/one-fn))
    (swap! tests-run conj (f/other-fn))
    (mfn/verifying
      (f/one-fn) :multiple-verifying-forms-pt1 (mfn.m/exactly 1))
    (mfn/verifying
      (f/other-fn) :multiple-verifying-forms-pt2 (mfn.m/exactly 1))))

(mfn/deftest deftest-tolerates-non-list-body-forms-test
  :non-list-form
  (mfn/testing "tolerates non-list body forms"
    :non-list-form
    (swap! tests-run conj :tolerates-non-list-body-forms)))

(def expected-tests-run
  #{:deftest
    :deftest-providing
    :deftest-providing+verifying+verifying-eventually-pt1
    :deftest-providing+verifying+verifying-eventually-pt2
    :deftest-providing+verifying+verifying-eventually-pt3
    :deftest-providing+verifying-pt1
    :deftest-providing+verifying-pt2
    :deftest-testing-pt1
    :deftest-testing-pt2
    :deftest-verifying
    :deftest-verifying-eventually
    :invoke-fn
    :multiple-providing-forms-pt1
    :multiple-providing-forms-pt2
    :multiple-verifying-forms-pt1
    :multiple-verifying-forms-pt2
    :private-fn-providing
    :private-fn-verifying
    :raise
    :testing
    :testing-providing
    :testing-providing+verifying+verifying-eventually-pt1
    :testing-providing+verifying+verifying-eventually-pt2
    :testing-providing+verifying+verifying-eventually-pt3
    :testing-providing+verifying-pt1
    :testing-providing+verifying-pt2
    :testing-verifying
    :testing-verifying-eventually
    :tolerates-non-list-body-forms})

(defn teardown []
  (is (= @tests-run expected-tests-run))
  (reset! tests-run #{}))

(defn once-fixture [f]
  (f) (teardown))

(use-fixtures :once once-fixture)
