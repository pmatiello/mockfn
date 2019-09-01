(ns mockfn.internal.mock
  (:require [mockfn.internal.utils :as utils]
            [mockfn.matchers :as matchers]))

(defrecord Calling [function])

(defrecord CallingOriginal [])

(defn- matches-arg?
  "Given a matcher expected and an argument arg, verify whether there is a
  match.

  If expected is a plain value instead of an implementation of
  mockfn.matchers/Matcher, use a simple equality check to verify matching."
  [[expected arg]]
  (if (satisfies? matchers/Matcher expected)
    (matchers/matches? expected arg)
    (= expected arg)))

(defn- matches-args?
  "Given a list o matchers expected and a list of values args, verify whether
  every value in args matches the corresponding (by position in sequence)
  matcher in expected."
  [expected args]
  (let [arity-matches?    (= (count expected) (count args))
        each-arg-matches? (every? matches-arg? (map vector expected args))]
    (and arity-matches? each-arg-matches? expected)))

(defn- for-args [m args]
  "Takes a map m where the keys are lists of matchers. Retrieves from this
  map a value for which the list args fulfill the list of matchers in the key.

  If args doesn't satisfy any list of matchers, returns ::unexpected-call."
  (if-let [expected (some #(matches-args? % args) (keys m))]
    (get m expected)
    ::unexpected-call))

(defn- func-or-unbound-var [func]
  "Returns the given function or an \"<unbound var>\" string if the
  function is nil (cljs doesn't have unbound vars)."
  (or func "<unbound var>"))

(defn- unexpected-call-msg [func args]
  "Exception message for unexpected call."
  (utils/formatted "Unexpected call to %s with args %s"
                   (func-or-unbound-var func) args))

(defn- ensure-expected-call [func spec args]
  "Throws an exception if the given call is unexpected."
  (when (-> spec :return-values (for-args args) #{::unexpected-call})
    (throw (ex-info (unexpected-call-msg func args) {}))))

(defn- increase-call-count [spec args]
  "Tracks the number of times a specific call is performed."
  (-> spec :times-called (for-args args) (swap! inc)))

(defn call->ret-val [func spec args]
  "Produces the return value for a mocked call.

  Tracks the number of times a specific call is made for a given function
  and set of parameters fulfilling matcher criteria.
  Throws an exception when an unexpected call is received."
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
  "Exception message for call count not matching the expectation."
  (utils/formatted "Expected %s with arguments %s %s, received %s."
                   (func-or-unbound-var func)
                   args
                   (matchers/description matcher)
                   times-called))

(defn mock->spec [mock]
  "Retrieves the specification for the given mock."
  (cond-> mock
          (var? mock) deref
          :then meta))
