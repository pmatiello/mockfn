(ns me.pmatiello.mockfn.fixtures)

(declare one-fn)
(declare other-fn)
(declare another-fn)
(declare ^:private pvt-fn)

(defn same-val [x] x)

(defprotocol SomeProtocol
  (m1 [this])
  (m2 [this x] [this x y]))
