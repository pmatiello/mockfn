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

  (testing "mocks multiple functions at once"
    (plain/providing
      [(f/one-fn) :one-fn
       (f/other-fn) :other-fn]
      (is (= :one-fn (f/one-fn)))
      (is (= :other-fn (f/other-fn))))))

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
            (is (= :one-fn (f/one-fn))))))))
