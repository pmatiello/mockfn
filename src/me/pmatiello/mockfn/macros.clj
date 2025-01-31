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
    (fn [acc [[func & args] ret-val & expected]]
      (-> acc
          (assoc-in [func :fn] func)
          (assoc-in [func :args (into [] args)]
                    {:ret-val  ret-val
                     :calls    `(atom 0)
                     :expected (into [] expected)})))
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
