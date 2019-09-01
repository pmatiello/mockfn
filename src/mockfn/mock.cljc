(ns mockfn.mock
  (:require [mockfn.internal.mock :as internal.mock]
            [mockfn.matchers :as matchers]
            [mockfn.parser]
            [mockfn.utils :as utils]))

(def ->Calling internal.mock/->Calling)

(def ->CallingOriginal internal.mock/->CallingOriginal)

(defn mock [func spec]
  (with-meta
    (fn [& args] (internal.mock/return-value-for-call func spec (into [] args)))
    spec))

(defn verify [mock]
  (let [meta (internal.mock/mock->meta mock)]
    (doseq [args    (-> meta :times-expected keys)
            matcher (-> meta :times-expected (get args))]
      (let [times-called (-> meta :times-called (get args) deref)]
        (when-not (matchers/matches? matcher times-called)
          (throw (ex-info (internal.mock/doesnt-match (-> meta :function) args matcher times-called) {})))))))
