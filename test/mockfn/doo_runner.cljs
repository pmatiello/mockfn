(ns mockfn.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            mockfn.macros-test
            mockfn.matchers-test
            mockfn.mock-test))

(enable-console-print!)

(doo-tests 'mockfn.macros-test
           'mockfn.matchers-test
           'mockfn.mock-test)
