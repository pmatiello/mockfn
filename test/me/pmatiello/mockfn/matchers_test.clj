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
      (is (= "｢exactly 1｣" (matchers/description exactly))))))

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

(deftest str-starts-with-test
  (let [str-starts-with (matchers/str-starts-with "prefix")]
    (testing "returns whether actual starts with expected prefix"
      (is (true? (matchers/matches? str-starts-with "prefix-str")))
      (is (false? (matchers/matches? str-starts-with "no-prefix"))))

    (testing "provides an informative string representation"
      (is (= "｢str-starts-with \"prefix\"｣" (matchers/description str-starts-with))))))

(deftest str-ends-with-test
  (let [str-ends-with (matchers/str-ends-with "suffix")]
    (testing "returns whether actual ends with expected suffix"
      (is (true? (matchers/matches? str-ends-with "str-suffix")))
      (is (false? (matchers/matches? str-ends-with "suffix-no"))))

    (testing "provides an informative string representation"
      (is (= "｢str-ends-with \"suffix\"｣" (matchers/description str-ends-with))))))

(deftest str-includes-test
  (let [str-includes (matchers/str-includes "substring")]
    (testing "returns whether actual includes the expected substring"
      (is (true? (matchers/matches? str-includes "with substring here")))
      (is (false? (matchers/matches? str-includes "no match here"))))

    (testing "provides an informative string representation"
      (is (= "｢str-includes \"substring\"｣" (matchers/description str-includes))))))

(deftest str-rexp-test
  (let [str-rexp (matchers/str-rexp #"^prefix.*suffix$")]
    (testing "returns whether actual matches the expected regular expression"
      (is (true? (matchers/matches? str-rexp "prefix-middle-suffix")))
      (is (false? (matchers/matches? str-rexp "no-match"))))

    (testing "provides an informative string representation"
      (is (= "｢str-rexp #\"^prefix.*suffix$\"｣" (matchers/description str-rexp))))))

(deftest pred-test
  (let [pred (matchers/pred even?)]
    (testing "matches actual satisfying the predicate"
      (is (true? (matchers/matches? pred 2)))
      (is (false? (matchers/matches? pred 3))))

    (testing "provides an informative string representation"
      (is (= "｢pred clojure.core$even_QMARK_｣" (matchers/description pred))))))

(deftest coll-empty-test
  (let [coll-empty (matchers/coll-empty)]
    (testing "returns whether actual is an empty collection"
      (is (true? (matchers/matches? coll-empty [])))
      (is (true? (matchers/matches? coll-empty '())))
      (is (true? (matchers/matches? coll-empty #{})))
      (is (true? (matchers/matches? coll-empty {})))
      (is (false? (matchers/matches? coll-empty [1])))
      (is (false? (matchers/matches? coll-empty {:key "value"}))))

    (testing "provides an informative string representation"
      (is (= "｢coll-empty｣" (matchers/description coll-empty))))))
