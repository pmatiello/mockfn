(ns me.pmatiello.mockfn.matchers-test
  (:require [clojure.test :refer :all]
            [me.pmatiello.mockfn.matchers :as matchers])
  (:import (clojure.lang Keyword)))

(deftest exactly-test
  (let [exactly (matchers/exactly 1)]
    (testing "returns whether actual is equal to expected"
      (is (true? (matchers/matches? exactly 1)))
      (is (false? (matchers/matches? exactly 2))))

    (testing "provides an informative string representation"
      (is (= "exactly 1" (matchers/description exactly))))))

(deftest at-least-test
  (let [at-least (matchers/at-least 2)]
    (testing "returns whether actual is at least equal to expected"
      (is (true? (matchers/matches? at-least 2)))
      (is (true? (matchers/matches? at-least 3)))
      (is (false? (matchers/matches? at-least 1))))

    (testing "provides an informative string representation"
      (is (= "at-least 2" (matchers/description at-least))))))

(deftest at-most-test
  (let [at-most (matchers/at-most 2)]
    (testing "returns whether actual is at most equal to expected"
      (is (true? (matchers/matches? at-most 1)))
      (is (true? (matchers/matches? at-most 2)))
      (is (false? (matchers/matches? at-most 3))))

    (testing "provides an informative string representation"
      (is (= "at-most 2" (matchers/description at-most))))))

(deftest any-test
  (let [any (matchers/any)]
    (testing "always matches actual"
      (is (true? (matchers/matches? any :anything)))
      (is (true? (matchers/matches? any "any thing")))
      (is (true? (matchers/matches? any 01234567890M))))

    (testing "provides an informative string representation"
      (is (= "any" (matchers/description any))))))

(deftest a-test
  (let [a (matchers/a Keyword)]
    (testing "matches actual of the expected type"
      (is (true? (matchers/matches? a :keyword)))
      (is (false? (matchers/matches? a "string"))))

    (testing "provides an informative string representation"
      (is (= "a clojure.lang.Keyword" (matchers/description a))))))

(deftest str-starts-with-test
  (let [starts-with (matchers/str-starts-with "prefix")]
    (testing "returns whether actual starts with expected prefix"
      (is (true? (matchers/matches? starts-with "prefix-str")))
      (is (false? (matchers/matches? starts-with "no-prefix"))))

    (testing "provides an informative string representation"
      (is (= "str-starts-with \"prefix\"" (matchers/description starts-with))))))

(deftest str-ends-with-test
  (let [ends-with (matchers/str-ends-with "suffix")]
    (testing "returns whether actual ends with expected suffix"
      (is (true? (matchers/matches? ends-with "str-suffix")))
      (is (false? (matchers/matches? ends-with "suffix-no"))))

    (testing "provides an informative string representation"
      (is (= "str-ends-with \"suffix\"" (matchers/description ends-with))))))

(deftest str-includes-test
  (let [includes (matchers/str-includes "substring")]
    (testing "returns whether actual includes the expected substring"
      (is (true? (matchers/matches? includes "with substring here")))
      (is (false? (matchers/matches? includes "no match here"))))

    (testing "provides an informative string representation"
      (is (= "str-includes \"substring\"" (matchers/description includes))))))

(deftest str-rexp-test
  (let [rexp (matchers/str-rexp #"^prefix.*suffix$")]
    (testing "returns whether actual matches the expected regular expression"
      (is (true? (matchers/matches? rexp "prefix-middle-suffix")))
      (is (false? (matchers/matches? rexp "no-match"))))

    (testing "provides an informative string representation"
      (is (= "str-rexp #\"^prefix.*suffix$\"" (matchers/description rexp))))))
