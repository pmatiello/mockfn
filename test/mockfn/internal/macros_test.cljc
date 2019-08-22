(ns mockfn.internal.macros-test
  (:require [clojure.test :refer [deftest testing is]]
            [mockfn.internal.macros :as internal.macros]
            [mockfn.fixtures :as fixtures]))

(def specification
  (internal.macros/bindings->specification
    [['(fixtures/one-fn) :one-fn-return]
     ['(fixtures/one-fn :arg) :one-fn-arg-return]
     ['(fixtures/other-fn) :other-fn-return 'call-count-matcher]]))

(deftest bindings->specification-test
  (testing "output includes every function in input"
    (is (= ['fixtures/one-fn 'fixtures/other-fn]
           (keys specification))))

  (testing "output includes functions"
    (is (= 'fixtures/one-fn
           (get-in specification ['fixtures/one-fn :function])))
    (is (= 'fixtures/other-fn
           (get-in specification ['fixtures/other-fn :function]))))

  (testing "output includes return values for each function and argument list"
    (is (= :one-fn-return
           (get-in specification ['fixtures/one-fn :return-values []])))
    (is (= :one-fn-arg-return
           (get-in specification ['fixtures/one-fn :return-values [:arg]])))
    (is (= :other-fn-return
           (get-in specification ['fixtures/other-fn :return-values []]))))

  (testing "output includes atoms for counting calls for each function and argument list"
    (is (= '(clojure.core/atom 0)
           (get-in specification ['fixtures/one-fn :times-called []])))
    (is (= '(clojure.core/atom 0)
           (get-in specification ['fixtures/one-fn :times-called [:arg]])))
    (is (= '(clojure.core/atom 0)
           (get-in specification ['fixtures/other-fn :times-called []]))))

  (testing "output includes the call-count matcher for each function and argument list"
    (is (= []
           (get-in specification ['fixtures/one-fn :times-expected []])))
    (is (= []
           (get-in specification ['fixtures/one-fn :times-expected [:arg]])))
    (is (= ['call-count-matcher]
           (get-in specification ['fixtures/other-fn :times-expected []])))))
