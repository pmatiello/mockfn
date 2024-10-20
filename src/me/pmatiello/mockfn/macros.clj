(ns me.pmatiello.mockfn.macros
  (:require [me.pmatiello.mockfn.mock :as mock]))

(defn- as-redefs
  [func->definition]
  (->> func->definition
       (map (fn [[func definition]] [func `(mock/mock ~func ~definition)]))
       (apply concat)))

(defn- func->spec
  [bindings]
  (reduce
    (fn [acc [[func & args] ret-val & times-expected]]
      (-> acc
          (assoc-in [func :function] func)
          (assoc-in [func :return-values (into [] args)] ret-val)
          (assoc-in [func :times-called (into [] args)] `(atom 0))
          (assoc-in [func :times-expected (into [] args)] (into [] times-expected))))
    {} bindings))

(defmacro providing
  "Mocks functions."
  [bindings & body]
  `(with-redefs ~(->> bindings (partition 2) func->spec as-redefs)
     ~@body))

(defmacro verifying
  "Mocks functions and verifies calls."
  [bindings & body]
  (let [specs# (->> bindings (partition 3) func->spec)]
    `(with-redefs ~(as-redefs specs#)
       ~@body
       (doseq [mock# (keys ~specs#)] (mock/verify mock#)))))
