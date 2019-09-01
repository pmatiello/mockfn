(ns mockfn.internal.mock
  (:require [mockfn.internal.utils :as utils]
            [mockfn.matchers :as matchers]))

(defrecord Calling [function])

(defrecord CallingOriginal [])

(defn- matches-arg?
  [[expected arg]]
  (if (satisfies? matchers/Matcher expected)
    (matchers/matches? expected arg)
    (= expected arg)))

(defn- matches-args?
  [expected args]
  (let [arity-matches?    (= (count expected) (count args))
        each-arg-matches? (every? matches-arg? (map vector expected args))]
    (and arity-matches? each-arg-matches? expected)))

(defn- for-args [m args]
  (if-let [expected (some #(matches-args? % args) (keys m))]
    (get m expected)
    ::unexpected-call))

(defn- func-or-unbound-var [func]
  ;; cljs doesn't have unbound vars
  #?(:clj func :cljs (or func "<unbound var>")))

(defn- unexpected-call-msg [func args]
  (utils/formatted "Unexpected call to %s with args %s"
                   (func-or-unbound-var func) args))

(defn- ensure-expected-call [func spec args]
  (when (-> spec :return-values (for-args args) #{::unexpected-call})
    (throw (ex-info (unexpected-call-msg func args) {}))))

(defn- increase-call-count [spec args]
  (-> spec :times-called (for-args args) (swap! inc)))

(defn call->ret-val [func spec args]
  (ensure-expected-call func spec args)
  (increase-call-count spec args)

  (let [ret-val (-> spec :return-values (for-args args))]
    (cond
      (instance? Calling ret-val)
      (-> ret-val :function (apply args))

      (instance? CallingOriginal ret-val)
      (-> spec :function (apply args))

      :default
      ret-val)))

(defn matcher-failure-ex-msg [func args matcher times-called]
  (utils/formatted "Expected %s with arguments %s %s, received %s."
                   (func-or-unbound-var func)
                   args
                   (matchers/description matcher)
                   times-called))

(defn mock->spec [mock]
  (cond-> mock
          (var? mock) deref
          :then meta))
