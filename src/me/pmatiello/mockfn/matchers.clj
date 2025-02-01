(ns me.pmatiello.mockfn.matchers
  (:require [clojure.string :as str]))

(defrecord Matcher [name function expected args-desc-fn])

(defn matches? [matcher actual]
  ((:function matcher) actual (:expected matcher)))

(defn description [matcher]
  (let [m-name       (:name matcher)
        args-desc-fn (:args-desc-fn matcher)
        expected     (-> matcher :expected args-desc-fn)]
    (->> [m-name expected]
         (filter some?)
         (str/join " "))))

(defn make
  ([name function expected] (make name function expected identity))
  ([name function expected description-fn] (->Matcher name function expected description-fn)))

(defn exactly [expected]
  (make "exactly" = expected))

(defn at-least [expected]
  (make "at least" >= expected))

(defn at-most [expected]
  (make "at most" <= expected))

(defn any []
  (make "any" (constantly true) nil))

(defn a [expected]
  (make "a" #(instance? %2 %1) expected pr-str))
