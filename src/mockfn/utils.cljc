(ns mockfn.utils
  #?(:cljs
     (:require
       [goog.string :as gstring]
       [goog.string.format])))

(def formatter
  #?(:clj format
     :cljs gstring/format))

