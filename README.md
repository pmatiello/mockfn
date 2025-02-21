# me.pmatiello/mockfn

This is a library for mockist test-driven-development in Clojure. It is meant to
be used alongside a regular testing framework such as `clojure.test`.

[![Clojars Project](https://img.shields.io/clojars/v/me.pmatiello/mockfn.svg)](https://clojars.org/me.pmatiello/mockfn)

## Usage

Instructions for using this library.

### Framework-agnostic usage

In order to use `mockfn`, it's enough to require it in a test namespace.

```clj
(:require [me.pmatiello.mockfn.plain :as mfn]
          [me.pmatiello.mockfn.matchers :as mfn.m]
          ...)
```

This will bring `mockfn` features in scope for this namespace.

#### Stubbing Function Calls

The `providing` macro replaces functions with mocks. These mocks return
preconfigured values when called with the expected arguments.

```clj
(testing "providing"
  (mfn/providing [(one-fn) :result]
    (is (= :result (one-fn)))))
```

As demonstrated below, a mock (`one-fn`) can be configured with different return
values for different arguments.

```clj
(testing "providing - one function, different arguments"
  (mfn/providing [(one-fn :argument-1) :result-1
                  (one-fn :argument-2) :result-2]
    (is (= :result-1 (one-fn :argument-1)))
    (is (= :result-2 (one-fn :argument-2)))))
```

It's also possible to configure multiple mocks, for multiple functions, at once.

```clj
(testing "providing with more than one function"
  (mfn/providing [(one-fn :argument) :result-1
                  (other-fn :argument) :result-2]
    (is (= :result-1 (one-fn :argument)))
    (is (= :result-2 (other-fn :argument))))))
```

#### Verifying Interactions

The `verifying` macro works similarly, but also defines an expectation for the
number of times a call should be performed during the test. A test will fail if
this expectation is not met.

```clj
(testing "verifying"
  (mfn/verifying [(one-fn :argument) :result (mfn.m/exactly 1)]
    (is (= :result (one-fn :argument)))))
```

Notice that the expected number of calls is defined using a
[matcher](#built-in-matchers).

#### Argument Matchers

Mocks can be configured to return a specific value for a range of different
arguments through [matchers](#built-in-matchers).

```clj
(testing "argument matchers"
  (mfn/providing [(one-fn (mfn.m/at-least 10) (mfn.m/at-most 20)) 15]
    (is (= 15 (one-fn 12 18))))))
```

### Syntax sugar for clojure.test

Support
for [clojure.test](https://clojure.github.io/clojure/clojure.test-api.html)
is provided in the `mockfn.clj-test` namespace.

```clj
(:require [clojure.test :refer :all]
          [me.pmatiello.mockfn.clj-test :as mfn]
          [me.pmatiello.mockfn.matchers :as mfn.m]
          ...)
```

The `mockfn.clj-test/deftest` and `mockfn.clj-test/testing` macros replace
`clojure.test/deftest` and `clojure.test/testing` and support a flatter (as in
not nested) mocking style using `mockfn.clj-test/providing` and
`mockfn.clj-test/verifying`:

```clj
(mfn/deftest deftest-with-builtin-mocking
  (is (= :one-fn (one-fn)))
  (mfn/providing
    (one-fn) :one-fn)

  (mfn/testing "testing with built-in-mocking"
    (is (= :one-fn (one-fn)))
    (is (= :other-fn (other-fn)))
    (mfn/verifying
      (other-fn) :other-fn (mfn.m/exactly 1))))
```

Note that to leverage the built-in support for mocking in these macros, it's
necessary to use the `providing` and `verifying` versions provided in the
`mockfn.clj-test` namespace.

### Built-in Matchers

The following matchers are included in `mockfn`:

| Matcher           | Description                                                            | Usage                        |
|-------------------|------------------------------------------------------------------------|------------------------------|
| `exactly`         | Matches if actual value is equal to the expected value.                | `(exactly expected)`         |
| `at-least`        | Matches if actual value is greater or equal than the expected value.   | `(at-least expected)`        |
| `at-most`         | Matches if actual value is less or equal than the expected value.      | `(at-most expected)`         |
| `any`             | Always matches.                                                        | `(any)`                      |
| `a`               | Matches if actual value is an instance of the expected type.           | `(a expected)`               |
| `str-starts-with` | Matches if actual string starts with the expected prefix.              | `(str-starts-with expected)` |
| `str-ends-with`   | Matches if actual string ends with the expected suffix.                | `(str-ends-with expected)`   |
| `str-includes`    | Matches if actual string includes the expected substring.              | `(str-includes expected)`    |
| `str-rexp`        | Matches if the expected regular expression matches the actual string.  | `(str-rexp expected)`        |
| `pred`            | Matches if the actual value satisfies the provided predicate function. | `(pred pred-fn)`             |

All matchers above are available in the `me.pmatiello.mockfn.matchers`
namespace.

## Quirks and Limitations

While `providing` and `verifying` calls can be nested, all required stubs and
expectations for a single mock must be defined in the same call. Mocking a
function in an inner `providing` or `verifying` call will override any
definitions made in the outer scope for the tests being run in the inner scope.

```clj
(testing "nested mocks"
  (mfn/providing [(one-fn :argument-1) :result-1]
    (mfn/providing [(one-fn :argument-2) :result-2
                    (other-fn :argument-3) :result-3]
      (is (thrown? ExceptionInfo (one-fn :argument-1)))
      (is (= :result-2 (one-fn :argument-2)))
      (is (= :result-3 (other-fn :argument-3))))
    (is (= :result-1 (one-fn :argument-1))))))
```

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

Distributed under the Eclipse Public License either version 2.0 or (at your
option) any later version.
