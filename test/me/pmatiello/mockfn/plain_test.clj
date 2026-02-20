(ns me.pmatiello.mockfn.plain-test
  (:require [clojure.test :refer :all]
            [me.pmatiello.mockfn.fixtures :as f]
            [me.pmatiello.mockfn.matchers :as matchers]
            [me.pmatiello.mockfn.plain :as plain])
  (:import (clojure.lang ExceptionInfo Keyword)))

(deftest providing-test
  (testing "mocks functions without arguments"
    (plain/providing
      [(f/one-fn) :mocked]
      (is (= :mocked (f/one-fn)))))

  (testing "mocks functions with arguments"
    (plain/providing
      [(f/one-fn :expected) :mocked
       (f/one-fn :expected :also-expected) :also-mocked]
      (is (= :mocked (f/one-fn :expected)))
      (is (= :also-mocked (f/one-fn :expected :also-expected)))
      (is (thrown-with-msg?
            ExceptionInfo #"Unexpected call"
            (f/one-fn :unexpected)))
      (is (thrown-with-msg?
            ExceptionInfo #"Unexpected call"
            (f/one-fn)))))

  (testing "mocks functions with argument matchers"
    (plain/providing
      [(f/one-fn (matchers/a Keyword)) :mocked]
      (is (= :mocked (f/one-fn :expected)))
      (is (thrown-with-msg?
            ExceptionInfo #"Unexpected call"
            (f/one-fn "unexpected")))))

  (testing "mocks functions with collection-valued arguments"
    (plain/providing
      [(f/one-fn [1 2]) :mocked]
      (is (= :mocked (f/one-fn [1 2])))))

  (testing "mocks multiple functions at once"
    (plain/providing
      [(f/one-fn) :one-fn
       (f/other-fn) :other-fn]
      (is (= :one-fn (f/one-fn)))
      (is (= :other-fn (f/other-fn)))))

  (testing "mocks private functions"
    (plain/providing
      [(#'f/pvt-fn) :mocked]
      (is (= :mocked (#'f/pvt-fn)))))

  (testing "supports dynamic return values"
    (plain/providing
      [(f/one-fn (matchers/any)) (plain/invoke identity)]
      (is (= :x (f/one-fn :x)))
      (is (= :y (f/one-fn :y)))))

  (testing "supports throwing exceptions"
    (plain/providing
      [(f/one-fn (matchers/any)) (plain/raise (ex-info "error!" {}))]
      (is (thrown-with-msg?
            ExceptionInfo #"error!"
            (f/one-fn :argument)))))

  (testing "supports calling original implementation"
    (plain/providing
      [(f/same-val (matchers/any)) (plain/invoke f/same-val)]
      (is (= :x (f/same-val :x)))
      (is (= :y (f/same-val :y)))))

  (testing "returns the evaluated body"
    (is (= :result
           (plain/providing [(f/one-fn) :result] (f/one-fn))))))

(deftest verifying-test
  (testing "mocks functions without arguments"
    (plain/verifying
      [(f/one-fn) :mocked (matchers/exactly 1)]
      (is (= :mocked (f/one-fn)))))

  (testing "mocks functions with arguments"
    (plain/verifying
      [(f/one-fn :expected) :mocked (matchers/exactly 1)
       (f/one-fn :expected :also-expected) :also-mocked (matchers/exactly 1)]
      (is (= :mocked (f/one-fn :expected)))
      (is (= :also-mocked (f/one-fn :expected :also-expected)))
      (is (thrown-with-msg?
            ExceptionInfo #"Unexpected call"
            (f/one-fn :unexpected)))
      (is (thrown-with-msg?
            ExceptionInfo #"Unexpected call"
            (f/one-fn)))))

  (testing "mocks functions with argument matchers"
    (plain/verifying
      [(f/one-fn (matchers/a Keyword)) :mocked (matchers/exactly 1)]
      (is (= :mocked (f/one-fn :expected)))
      (is (thrown-with-msg?
            ExceptionInfo #"Unexpected call"
            (f/one-fn "unexpected")))))

  (testing "mocks functions with collection-valued arguments"
    (plain/verifying
      [(f/one-fn [1 2]) :mocked (matchers/exactly 1)]
      (is (= :mocked (f/one-fn [1 2])))))

  (testing "mocks multiple functions at once"
    (plain/verifying
      [(f/one-fn) :one-fn (matchers/exactly 1)
       (f/other-fn) :other-fn (matchers/exactly 1)]
      (is (= :one-fn (f/one-fn)))
      (is (= :other-fn (f/other-fn)))))

  (testing "fails if calls are not performed the expected number of times"
    (is (thrown-with-msg?
          ExceptionInfo #"Expected call .*"
          (plain/verifying
            [(f/one-fn) :one-fn (matchers/exactly 2)]
            (is (= :one-fn (f/one-fn)))))))

  (testing "mocks private functions"
    (testing "returns configured values"
      (plain/verifying
        [(#'f/pvt-fn) :mocked (matchers/exactly 1)]
        (is (= :mocked (#'f/pvt-fn)))))

    (testing "validates the call count"
      (is (thrown-with-msg?
            ExceptionInfo #"Expected call .*"
            (plain/verifying
              [(#'f/pvt-fn) :pvt-fn (matchers/exactly 2)]
              (is (= :pvt-fn (#'f/pvt-fn))))))))

  (testing "supports dynamic return values"
    (plain/verifying
      [(f/one-fn (matchers/any)) (plain/invoke identity) (matchers/exactly 2)]
      (is (= :x (f/one-fn :x)))
      (is (= :y (f/one-fn :y)))))

  (testing "supports throwing exceptions"
    (plain/verifying
      [(f/one-fn (matchers/any)) (plain/raise (ex-info "error!" {})) (matchers/exactly 1)]
      (is (thrown-with-msg?
            ExceptionInfo #"error!"
            (f/one-fn :argument)))))

  (testing "supports calling original implementation"
    (plain/verifying
      [(f/same-val (matchers/any)) (plain/invoke f/same-val) (matchers/exactly 2)]
      (is (= :x (f/same-val :x)))
      (is (= :y (f/same-val :y)))))

  (testing "returns the evaluated body"
    (is (= :result
           (plain/verifying [(f/one-fn) :result (matchers/exactly 1)] (f/one-fn))))))
