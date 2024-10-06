(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'me.pmatiello/mockfn)
(def version "0.1.0")
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s-%s.jar" (name lib) version))

(defn clean [_]
      (b/delete {:path "target"}))

(defn jar [_]
      (b/write-pom {:class-dir class-dir
                    :lib       lib
                    :version   version
                    :basis     basis
                    :src-dirs  ["src"]
                    :scm       {:url                 "https://github.com/pmatiello/mockfn"
                                :connection          "scm:git:git://github.com/pmatiello/mockfn.git"
                                :developerConnection "scm:git:ssh://git@github.com:pmatiello/mockfn.git"
                                :tag                 (str "v" version)}})
      (b/copy-dir {:src-dirs   ["src"]
                   :target-dir class-dir})
      (b/jar {:class-dir class-dir
              :jar-file  jar-file}))
