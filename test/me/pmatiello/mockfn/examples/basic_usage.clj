(ns me.pmatiello.mockfn.examples.basic-usage
  (:require [clojure.test :refer :all]
            [me.pmatiello.mockfn.matchers :as mfn.m]
            [me.pmatiello.mockfn.plain :as mfn])
  (:import (clojure.lang ExceptionInfo)))

(def one-fn)
(def other-fn)
(def ^:private pvt-fn)

(deftest examples-test
  (testing "providing"
    (mfn/providing [(one-fn) :result]
      (is (= :result (one-fn)))))

  (testing "providing, private function"
    (mfn/providing [(#'pvt-fn) :result]
      (is (= :result (#'pvt-fn)))))

  (testing "providing - one function, different arguments"
    (mfn/providing [(one-fn :argument-1) :result-1
                    (one-fn :argument-2) :result-2]
      (is (= :result-1 (one-fn :argument-1)))
      (is (= :result-2 (one-fn :argument-2)))))

  (testing "providing with more than one function"
    (mfn/providing [(one-fn :argument) :result-1
                    (other-fn :argument) :result-2]
      (is (= :result-1 (one-fn :argument)))
      (is (= :result-2 (other-fn :argument)))))

  (testing "verifying"
    (mfn/verifying [(one-fn :argument) :result (mfn.m/exactly 1)]
      (is (= :result (one-fn :argument)))))

  (testing "verifying, private function"
    (mfn/verifying [(#'pvt-fn :argument) :result (mfn.m/exactly 1)]
      (is (= :result (#'pvt-fn :argument)))))

  (testing "argument matchers"
    (mfn/providing [(one-fn (mfn.m/at-least 10) (mfn.m/at-most 20)) 15]
      (is (= 15 (one-fn 12 18)))))

  (testing "nested mocks"
    (mfn/providing [(one-fn :argument-1) :result-1]
      (mfn/providing [(one-fn :argument-2) :result-2
                      (other-fn :argument-3) :result-3]
        (is (thrown? ExceptionInfo (one-fn :argument-1)))
        (is (= :result-2 (one-fn :argument-2)))
        (is (= :result-3 (other-fn :argument-3))))
      (is (= :result-1 (one-fn :argument-1))))))
