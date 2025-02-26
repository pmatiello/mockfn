(ns me.pmatiello.mockfn.matchers-test
  (:require [clojure.test :refer :all]
            [me.pmatiello.mockfn.matchers :as matchers])
  (:import (clojure.lang Keyword)))

(deftest any-test
  (let [any (matchers/any)]
    (testing "always matches actual"
      (is (true? (matchers/matches? any :anything)))
      (is (true? (matchers/matches? any "any thing")))
      (is (true? (matchers/matches? any 01234567890M))))

    (testing "provides an informative string representation"
      (is (= "｢any｣" (matchers/description any))))))

(deftest a-test
  (let [a (matchers/a Keyword)]
    (testing "matches actual of the expected type"
      (is (true? (matchers/matches? a :keyword)))
      (is (false? (matchers/matches? a "string"))))

    (testing "provides an informative string representation"
      (is (= "｢a clojure.lang.Keyword｣" (matchers/description a))))))

(deftest exactly-test
  (let [exactly (matchers/exactly 1)]
    (testing "returns whether actual is equal to expected"
      (is (true? (matchers/matches? exactly 1)))
      (is (false? (matchers/matches? exactly 2))))

    (testing "provides an informative string representation"
      (is (= "｢exactly 1｣" (matchers/description exactly))))))

(deftest empty-test
  (let [empty (matchers/empty)]
    (testing "returns whether actual is an empty collection"
      (is (true? (matchers/matches? empty nil)))
      (is (true? (matchers/matches? empty [])))
      (is (true? (matchers/matches? empty '())))
      (is (true? (matchers/matches? empty #{})))
      (is (true? (matchers/matches? empty {})))
      (is (false? (matchers/matches? empty [1])))
      (is (false? (matchers/matches? empty {:key "value"}))))

    (testing "returns whether actual is an empty string"
      (is (true? (matchers/matches? empty "")))
      (is (false? (matchers/matches? empty "not empty"))))

    (testing "provides an informative string representation"
      (is (= "｢empty｣" (matchers/description empty))))))

(deftest pred-test
  (let [pred (matchers/pred even?)]
    (testing "matches actual satisfying the predicate"
      (is (true? (matchers/matches? pred 2)))
      (is (false? (matchers/matches? pred 3))))

    (testing "provides an informative string representation"
      (is (= "｢pred clojure.core$even_QMARK_｣" (matchers/description pred))))))

(deftest at-least-test
  (let [at-least (matchers/at-least 2)]
    (testing "returns whether actual is at least equal to expected"
      (is (true? (matchers/matches? at-least 2)))
      (is (true? (matchers/matches? at-least 3)))
      (is (false? (matchers/matches? at-least 1))))

    (testing "provides an informative string representation"
      (is (= "｢at-least 2｣" (matchers/description at-least))))))

(deftest at-most-test
  (let [at-most (matchers/at-most 2)]
    (testing "returns whether actual is at most equal to expected"
      (is (true? (matchers/matches? at-most 1)))
      (is (true? (matchers/matches? at-most 2)))
      (is (false? (matchers/matches? at-most 3))))

    (testing "provides an informative string representation"
      (is (= "｢at-most 2｣" (matchers/description at-most))))))

(deftest starts-with-test
  (let [starts-with (matchers/starts-with "prefix")]
    (testing "returns whether actual starts with expected prefix"
      (is (true? (matchers/matches? starts-with "prefix-str")))
      (is (false? (matchers/matches? starts-with "no-prefix"))))

    (testing "provides an informative string representation"
      (is (= "｢starts-with \"prefix\"｣" (matchers/description starts-with))))))

(deftest ends-with-test
  (let [ends-with (matchers/ends-with "suffix")]
    (testing "returns whether actual ends with expected suffix"
      (is (true? (matchers/matches? ends-with "str-suffix")))
      (is (false? (matchers/matches? ends-with "suffix-no"))))

    (testing "provides an informative string representation"
      (is (= "｢ends-with \"suffix\"｣" (matchers/description ends-with))))))

(deftest includes-test
  (let [includes (matchers/includes "substring")]
    (testing "returns whether actual includes the expected substring"
      (is (true? (matchers/matches? includes "with substring here")))
      (is (false? (matchers/matches? includes "no match here"))))

    (testing "provides an informative string representation"
      (is (= "｢includes \"substring\"｣" (matchers/description includes))))))

(deftest regex-test
  (let [regex (matchers/regex #"^prefix.*suffix$")]
    (testing "returns whether actual matches the expected regular expression"
      (is (true? (matchers/matches? regex "prefix-middle-suffix")))
      (is (false? (matchers/matches? regex "no-match"))))

    (testing "provides an informative string representation"
      (is (= "｢regex #\"^prefix.*suffix$\"｣" (matchers/description regex))))))

(deftest contains-all-test
  (let [contains-all (matchers/contains-all [1 2])]
    (testing "returns whether actual collection contains all expected values"
      (is (true? (matchers/matches? contains-all [1 2 3])))
      (is (true? (matchers/matches? contains-all [2 1])))
      (is (false? (matchers/matches? contains-all [1 3])))
      (is (false? (matchers/matches? contains-all [3 4]))))

    (testing "provides an informative string representation"
      (is (= "｢contains-all #{1 2}｣" (matchers/description contains-all)))))

  (let [contains-all (matchers/contains-all [1 nil])]
    (testing "enforces presence of nil in actual"
      (is (true? (matchers/matches? contains-all [1 2 nil])))
      (is (false? (matchers/matches? contains-all [1 2]))))))

(deftest contains-any-test
  (let [contains-any (matchers/contains-any [1 2])]
    (testing "returns whether actual collection contains any expected values"
      (is (true? (matchers/matches? contains-any [1 3])))
      (is (true? (matchers/matches? contains-any [2 4])))
      (is (false? (matchers/matches? contains-any [3 4])))
      (is (false? (matchers/matches? contains-any []))))

    (testing "provides an informative string representation"
      (is (= "｢contains-any #{1 2}｣" (matchers/description contains-any)))))

  (let [coll-contains-any (matchers/contains-any [1 nil])]
    (testing "handles nil as regular values"
      (is (true? (matchers/matches? coll-contains-any [nil])))
      (is (false? (matchers/matches? coll-contains-any [2 3]))))))

(deftest not>-test
  (let [not> (matchers/not> (matchers/exactly 1))]
    (testing "returns whether actual does not match the provided matcher"
      (is (true? (matchers/matches? not> 2)))
      (is (false? (matchers/matches? not> 1))))

    (testing "provides an informative string representation"
      (is (= "｢not> ｢exactly 1｣｣" (matchers/description not>))))))

(deftest and>-test
  (let [and> (matchers/and> (matchers/at-least 2) (matchers/at-most 4))]
    (testing "returns whether actual matches all provided matchers"
      (is (true? (matchers/matches? and> 2)))
      (is (true? (matchers/matches? and> 3)))
      (is (true? (matchers/matches? and> 4)))
      (is (false? (matchers/matches? and> 1)))
      (is (false? (matchers/matches? and> 5))))

    (testing "provides an informative string representation"
      (is (= "｢and> ｢at-least 2｣ ｢at-most 4｣｣" (matchers/description and>))))))

(deftest or>-test
  (let [or> (matchers/or> (matchers/exactly 2) (matchers/exactly 3))]
    (testing "returns whether actual matches any of the provided matchers"
      (is (true? (matchers/matches? or> 2)))
      (is (true? (matchers/matches? or> 3)))
      (is (false? (matchers/matches? or> 5))))

    (testing "provides an informative string representation"
      (is (= "｢or> ｢exactly 2｣ ｢exactly 3｣｣" (matchers/description or>))))))
