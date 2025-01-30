# me.pmatiello/mockfn

This is a library for mockist test-driven-development in Clojure. It is meant to
be used alongside a regular testing framework such as `clojure.test`.

[![Clojars Project](https://img.shields.io/clojars/v/me.pmatiello/mockfn.svg)](https://clojars.org/me.pmatiello/mockfn)

## Usage

The `providing` macro replaces a function with a configured mock.

```clj
(deftest providing-test
  (providing
    [(one-fn) :mocked]
    (is (= :mocked (one-fn)))))
```

The `verifying` macro works similarly, but also defines an expectation for the
number of times a call should be performed during the test.

```clj
(deftest verifying-test
  (verifying
    [(one-fn) :mocked (at-least 1)]
    (is (= :mocked (one-fn)))))
```

Refer to the [documentation](doc/documentation.md) for more detailed
information, including:

- [Framework-agonostic usage](doc/documentation.md#framework-agonostic-usage)
- [Syntax sugar for
  `clojure.test`](doc/documentation.md#syntax-sugar-for-clojuretest)
- [Argument matchers](doc/documentation.md#argument-matchers)

## Development

Information for developing this library.

### Running tests

The following command will execute the unit tests:

```
% clj -X:test
```

### Building

The following command will build a jar file:

```
% clj -T:build jar
```

To clean a previous build, run:

```
% clj -T:build clean
```

### Releasing

Before releasing, update the library version in the [build.clj](./build.clj)
file.

Make a commit and generate a new tag:

```
% git commit -a -m "Release: ${VERSION}"
% git tag -a "v${VERSION}" -m "Release: ${VERSION}"
% git push
% git push origin "v${VERSION}" 
```

To release to [clojars](https://clojars.org), run:

```
% mvn deploy:deploy-file \
      -Dfile=target/mockfn-${VERSION}.jar \
      -DrepositoryId=clojars \
      -Durl=https://clojars.org/repo \
      -DpomFile=target/classes/META-INF/maven/me.pmatiello/mockfn/pom.xml
```

Notice that this step requires clojars to be configured as a server in the local
`~/.m2/settings.xml` file.

## Contribution Policy

This software is open-source, but closed to contributions.

## License

Distributed under the Eclipse Public License either version 1.0 or (at your
option) any later version.
