# me.pmatiello/mockfn

This is a library for mockist test-driven development in Clojure. It is meant to
be used alongside a regular testing framework such as `clojure.test`.

[![Clojars Project](https://img.shields.io/clojars/v/me.pmatiello/mockfn.svg)](https://clojars.org/me.pmatiello/mockfn)
[![Documentation](https://cljdoc.org/badge/me.pmatiello/mockfn)](https://cljdoc.org/d/me.pmatiello/mockfn)

## Usage

This library is designed to be independent from any testing framework. All
supported features are described below in
the [Framework-agnostic usage](#framework-agnostic-usage) section. For
convenience, syntactic sugar
for [clojure.test](https://clojure.github.io/clojure/clojure.test-api.html) is
also available and described in the
[Syntactic sugar for clojure.test](#syntactic-sugar-for-clojuretest) section.

### Framework-agnostic usage

In order to use `mockfn`, it's enough to require it in a test namespace.

```clj
(:require [me.pmatiello.mockfn.plain :as mfn]
          [me.pmatiello.mockfn.matchers :as mfn.m]
          ...)
```

This will bring `mockfn` features into scope for the namespace.

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

#### Mocking private functions

Private functions can be mocked by referring to the `Var` associated with the
symbol of the mocked function.

```clj
(testing "providing, private function"
  (mfn/providing [(#'pvt-fn) :result]
    (is (= :result (#'pvt-fn)))))
```

```clj
(testing "verifying, private function"
  (mfn/verifying [(#'pvt-fn :argument) :result (mfn.m/exactly 1)]
    (is (= :result (#'pvt-fn :argument)))))
```

#### Returning dynamic values

The `invoke` function allows mocks to dynamically invoke a function with the
received arguments and return the output.

```clj
(testing "returns dynamic values"
  (mfn/providing
    [(one-fn (matchers/any)) (mfn/invoke identity)]
    (is (= :x (#'one-fn :x)))
    (is (= :y (#'one-fn :y)))))
```

The same approach can be used to invoke the original implementation to test
whether a function has been invoked as expected without overriding its
implementation:

```clj
(testing "invokes the original implementation"
  (mfn/verifying
    [(one-fn (matchers/any)) (mfn/invoke one-fn) (matchers/exactly 2)]
    (is (= :x (one-fn :x)))
    (is (= :y (one-fn :y)))))
```

#### Throwing exceptions

Mocks can be configured to throw exceptions when invoked using the `raise`
function. This can be used to simulate error scenarios in tests.

```clj
(testing "throws an exception"
  (mfn/providing 
    [(one-fn) (mfn/raise (ex-info "error!" {}))]
    (is (thrown-with-msg? ExceptionInfo #"error!" (one-fn)))))
```

#### Returning different values at each invocation

The `me.pmatiello.mockfn.xtras/return-in-order` function allows a mock to return
a sequence of values in the order they are called. After the last value, it will
continue from the first value in the sequence.

```clj
(testing "returns values in order"
  (mfn/providing
    [(one-fn) (xtras/return-in-order [:a :b :c])]
    (is (= [:a :b :c :a :b] (repeatedly 5 one-fn))))
```

### Syntactic sugar for clojure.test

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

| Matcher        | Description                                                            | Usage                   |
|----------------|------------------------------------------------------------------------|-------------------------|
| **Generic**    |                                                                        |                         |
| `any`          | Always matches.                                                        | `(any)`                 |
| `a`            | Matches if actual value is an instance of the expected type.           | `(a type)`              |
| `exactly`      | Matches if actual value is equal to the expected value.                | `(exactly value)`       |
| `empty`        | Matches if the actual value is empty.                                  | `(empty)`               |
| `pred`         | Matches if the actual value satisfies the provided predicate function. | `(pred pred-fn)`        |
| **Boolean**    |                                                                        |                         |
| `truthy`       | Matches if the actual value is truthy.                                 | `(truthy)`              |
| `falsy`        | Matches if the actual value is falsy.                                  | `(falsy)`               |
| **Numeric**    |                                                                        |                         |
| `at-least`     | Matches if actual value is greater or equal than the expected value.   | `(at-least value)`      |
| `at-most`      | Matches if actual value is less or equal than the expected value.      | `(at-most value)`       |
| `between`      | Matches if actual value is between the lower and upper bounds.         | `(between lower upper)` |
| **String**     |                                                                        |                         |
| `starts-with`  | Matches if actual string starts with the expected prefix.              | `(starts-with prefix)`  |
| `ends-with`    | Matches if actual string ends with the expected suffix.                | `(ends-with suffix)`    |
| `includes`     | Matches if actual string includes the expected substring.              | `(includes substring)`  |
| `regex`        | Matches if the expected regular expression matches the actual string.  | `(regex expression)`    |
| **Collection** |                                                                        |                         |
| `contains-all` | Matches if the actual collection contains all expected values.         | `(contains values)`     |
| `contains-any` | Matches if the actual collection contains any expected values.         | `(contains-any values)` |
| **Operators**  |                                                                        |                         |
| `not>`         | Matches if the actual value does not match the provided matcher.       | `(not> matcher)`        |
| `and>`         | Matches if the actual value matches all provided matchers.             | `(and> m1 m2 ...)`      |
| `or>`          | Matches if the actual value matches any of the provided matchers.      | `(or> m1 m2 ...)`       |

All matchers above are available in the `me.pmatiello.mockfn.matchers`
namespace.

## Quirks and Limitations

While `providing` and `verifying` calls can be nested, all required stubs and
expectations for a single mock must be defined within the same call. Mocking a
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
file and the release date in the changelog.

Make a commit and generate a new tag:

```
% git commit -a -m "Release: ${VERSION}"
% git tag -a "v${VERSION}" -m "Release: ${VERSION}"
% git push
% git push origin "v${VERSION}" 
```

Run all tests and build the release artifact:

```
% clj -X:test
% clj -T:build clean
% clj -T:build jar
```

To release to [clojars](https://clojars.org), run:

```
% mvn deploy:deploy-file \
      -Dfile=target/mockfn-${VERSION}.jar \
      -DrepositoryId=clojars \
      -Durl=https://clojars.org/repo \
      -DpomFile=target/classes/META-INF/maven/me.pmatiello/mockfn/pom.xml
```

Note that this step requires clojars to be configured as a server in the local
`~/.m2/settings.xml` file.

## Contribution Policy

This software is open-source, but closed to contributions.

## License

Distributed under the Eclipse Public License either version 2.0 or (at your
option) any later version.
