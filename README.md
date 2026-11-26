# me.pmatiello/mockfn

This is a mocking library for test-driven development in Clojure. It is meant to
be used alongside a regular testing framework such as `clojure.test`.

[![Clojars Project](https://img.shields.io/clojars/v/me.pmatiello/mockfn.svg)](https://clojars.org/me.pmatiello/mockfn)
[![Documentation](https://cljdoc.org/badge/me.pmatiello/mockfn)](https://cljdoc.org/d/me.pmatiello/mockfn)

<!-- TOC -->

* [me.pmatiello/mockfn](#mepmatiellomockfn)
    * [Usage](#usage)
        * [Framework-agnostic usage](#framework-agnostic-usage)
            * [Stubbing function calls](#stubbing-function-calls)
            * [Verifying interactions](#verifying-interactions)
            * [Verifying asynchronous interactions](#verifying-asynchronous-interactions)
            * [Argument matchers](#argument-matchers)
            * [Mocking private functions](#mocking-private-functions)
            * [Returning dynamic values](#returning-dynamic-values)
            * [Throwing exceptions](#throwing-exceptions)
        * [Syntactic sugar for clojure.test](#syntactic-sugar-for-clojuretest)
            * [Stubbing function calls](#stubbing-function-calls-1)
            * [Verifying interactions](#verifying-interactions-1)
            * [Verifying asynchronous interactions](#verifying-asynchronous-interactions-1)
            * [Argument matchers](#argument-matchers-1)
            * [Mocking private functions](#mocking-private-functions-1)
            * [Returning dynamic values](#returning-dynamic-values-1)
            * [Throwing exceptions](#throwing-exceptions-1)
        * [Matchers](#matchers)
            * [Built-in matchers](#built-in-matchers)
            * [Creating custom matchers](#creating-custom-matchers)
        * [Extra Features](#extra-features)
            * [Returning different values at each invocation](#returning-different-values-at-each-invocation)
            * [Verifying the order of interactions](#verifying-the-order-of-interactions)
            * [Mocking protocol methods](#mocking-protocol-methods)
    * [Quirks and Limitations](#quirks-and-limitations)
        * [No support for parallel test execution](#no-support-for-parallel-test-execution)
        * [Inner definitions override outer ones](#inner-definitions-override-outer-ones)
    * [Development](#development)
        * [Running tests](#running-tests)
        * [Building](#building)
        * [Releasing](#releasing)
    * [Contribution Policy](#contribution-policy)
    * [License](#license)

<!-- TOC -->

## Usage

This library is designed to be compatible with any testing framework. All
supported features are described below in the
[Framework-agnostic usage](#framework-agnostic-usage) section. For convenience,
special support for
[clojure.test](https://clojure.github.io/clojure/clojure.test-api.html) is also
available and described in the
[Syntactic sugar for clojure.test](#syntactic-sugar-for-clojuretest) section.

### Framework-agnostic usage

In order to use `mockfn`, it's enough to require it in a test namespace.

```clj
(:require [me.pmatiello.mockfn.plain :as mfn]
          [me.pmatiello.mockfn.matchers :as mfn.m]
          ...)
```

This makes `mockfn`'s features available in the namespace.

#### Stubbing function calls

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

#### Verifying interactions

The `verifying` macro works similarly, but also defines an expectation for the
number of times a call should be made during the test. A test will fail if this
expectation is not met.

```clj
(testing "verifying"
  (mfn/verifying [(one-fn :argument) :result (mfn.m/exactly 1)]
    (is (= :result (one-fn :argument)))))
```

Notice that the expected number of calls is defined using a
[matcher](#built-in-matchers).

#### Verifying asynchronous interactions

The `verifying-eventually` macro also defines call-count expectations, but this
verification is performed repeatedly, pausing for a specified interval between
checks, until all expectations are met or the maximum number of attempts is
exceeded.

```clj
(testing "verifying-eventually"
  (mfn/verifying-eventually
    {:max-attempts 10 :interval-ms 20}
    [(one-fn :argument) :result (mfn.m/exactly 1)]
    (future (Thread/sleep 50) (one-fn :argument))))
```

#### Argument matchers

Mocks can be configured to return a specific value when called with arguments
matching certain criteria through [matchers](#built-in-matchers).

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
received arguments and return its result.

```clj
(testing "returns dynamic values"
  (mfn/providing
    [(one-fn (mfn.m/any)) (mfn/invoke identity)]
    (is (= :x (#'one-fn :x)))
    (is (= :y (#'one-fn :y)))))
```

The same approach can be used to invoke the original implementation to test
whether a function has been invoked as expected without overriding its
implementation:

```clj
(testing "invokes the original implementation"
  (mfn/verifying
    [(one-fn (mfn.m/any)) (mfn/invoke one-fn) (mfn.m/exactly 2)]
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

### Syntactic sugar for clojure.test

The `mockfn.clj-test` namespace offers a more convenient interface for test
suites that use
[clojure.test](https://clojure.github.io/clojure/clojure.test-api.html).

This convenience is mostly provided through substitute `deftest` and `testing`
macros that allow mocks to be declared alongside the test code without nesting
the mocking macros around the test code.

```clj
(:require [clojure.test :refer :all]
          [me.pmatiello.mockfn.clj-test :as mfn]
          [me.pmatiello.mockfn.matchers :as mfn.m]
          ...)
```

#### Stubbing function calls

The `providing` form replaces functions with mocks. These mocks return
preconfigured values when called with the expected arguments.

```clj
(mfn/deftest providing-test
  (is (= :result (one-fn :argument)))
  (mfn/providing
    (one-fn :argument) :result))
```

It's also possible to configure multiple mocks at once.

```clj
(mfn/deftest providing-many-test
  (mfn/testing "providing with more than one function"
    (is (= :result-1 (one-fn :argument)))
    (is (= :result-2 (other-fn :argument)))
    (mfn/providing
      (one-fn :argument) :result-1
      (other-fn :argument) :result-2)))
```

#### Verifying interactions

The `verifying` form works similarly, but also defines an expectation for the
number of times a call should be made during the test. A test will fail if this
expectation is not met. The expected number of calls is defined using a
[matcher](#built-in-matchers).

```clj
(mfn/deftest verifying-test
  (is (= :result (one-fn :argument)))
  (mfn/verifying
    (one-fn :argument) :result (mfn.m/exactly 1)))
```

#### Verifying asynchronous interactions

The `verifying-eventually` form also defines call-count expectations, but this
verification is performed repeatedly, pausing for a specified interval between
checks, until all expectations are met or the maximum number of attempts is
exceeded.

```clj
(mfn/deftest verifying-eventually-test
  (future (Thread/sleep 50) (one-fn :argument))
  (mfn/verifying-eventually
    {:max-attempts 10 :interval-ms 20}
    (one-fn :argument) :result (mfn.m/exactly 1)))
```

#### Argument matchers

Mocks can be configured to return a specific value when called with arguments
matching certain criteria through [matchers](#built-in-matchers).

```clj
(mfn/deftest argument-matchers-test
  (is (= 15 (one-fn 12 18)))
  (mfn/providing
    (one-fn (mfn.m/at-least 10) (mfn.m/at-most 20)) 15))
```

#### Mocking private functions

Private functions can be mocked by referring to the `Var` associated with the
symbol of the mocked function.

```clj
(mfn/deftest private-function-test
  (is (= :result (#'pvt-fn)))
  (mfn/providing
    (#'pvt-fn) :result))
```

#### Returning dynamic values

The `invoke` function allows mocks to dynamically invoke a function with the
received arguments and return its result.

```clj
(mfn/deftest dynamic-values-test
  (is (= :x (one-fn :x)))
  (is (= :y (one-fn :y)))
  (mfn/providing
    (one-fn (mfn.m/any)) (mfn/invoke identity)))
```

The same approach can be used to invoke the original implementation to test
whether a function has been invoked as expected without overriding its
implementation:

```clj
(mfn/deftest original-implementation-test
  (is (= :x (one-fn :x)))
  (is (= :y (one-fn :y)))
  (mfn/verifying
    (one-fn (mfn.m/any)) (mfn/invoke one-fn) (mfn.m/exactly 2)))
```

#### Throwing exceptions

Mocks can be configured to throw exceptions when invoked using the `raise`
function. This can be used to simulate error scenarios in tests.

```clj
(mfn/deftest throwing-exception-test
  (is (thrown-with-msg? ExceptionInfo #"error!" (one-fn)))
  (mfn/providing
    (one-fn) (mfn/raise (ex-info "error!" {}))))
```

### Matchers

Matchers are used to match function arguments and to specify expected call
counts. A set of built-in matchers is provided out of the box to cover common
cases. Custom matchers can be created when more specific behavior is needed.

#### Built-in matchers

The following matchers are included in `mockfn`:

| Matcher        | Description                                                                 | Usage                   |
|----------------|-----------------------------------------------------------------------------|-------------------------|
| **Generic**    |                                                                             |                         |
| `any`          | Matches any value.                                                          | `(any)`                 |
| `a`            | Matches if the actual value is an instance of the expected type.            | `(a type)`              |
| `exactly`      | Matches if the actual value is equal to the expected value.                 | `(exactly value)`       |
| `pred`         | Matches if the actual value satisfies the provided predicate function.      | `(pred pred-fn)`        |
| **Presence**   |                                                                             |                         |
| `some`         | Matches if the actual value is not nil.                                     | `(some)`                |
| `null`         | Matches if the actual value is nil.                                         | `(null)`                |
| `empty`        | Matches if the actual value is empty (a collection, a string, etc.).        | `(empty)`               |
| **Boolean**    |                                                                             |                         |
| `truthy`       | Matches if the actual value is truthy.                                      | `(truthy)`              |
| `falsy`        | Matches if the actual value is falsy.                                       | `(falsy)`               |
| **Numeric**    |                                                                             |                         |
| `at-least`     | Matches if the actual value is greater than or equal to the expected value. | `(at-least value)`      |
| `at-most`      | Matches if the actual value is less than or equal to the expected value.    | `(at-most value)`       |
| `between`      | Matches if the actual value is between the lower and upper bounds.          | `(between lower upper)` |
| **String**     |                                                                             |                         |
| `starts-with`  | Matches if the actual string starts with the expected prefix.               | `(starts-with prefix)`  |
| `ends-with`    | Matches if the actual string ends with the expected suffix.                 | `(ends-with suffix)`    |
| `includes`     | Matches if the actual string includes the expected substring.               | `(includes substring)`  |
| `regex`        | Matches if the expected regular expression matches the actual string.       | `(regex expression)`    |
| **Collection** |                                                                             |                         |
| `contains-all` | Matches if the actual collection contains all expected values.              | `(contains-all values)` |
| `contains-any` | Matches if the actual collection contains any expected values.              | `(contains-any values)` |
| **Operators**  |                                                                             |                         |
| `not>`         | Matches if the actual value does not match the provided matcher.            | `(not> matcher)`        |
| `and>`         | Matches if the actual value matches all provided matchers.                  | `(and> m1 m2 ...)`      |
| `or>`          | Matches if the actual value matches any of the provided matchers.           | `(or> m1 m2 ...)`       |
| `*>`           | Matches if the provided matcher matches all remaining arguments.            | `(*> matcher)`          |

All matchers above are available in the `me.pmatiello.mockfn.matchers`
namespace.

#### Creating custom matchers

Custom matchers can be created through the `me.pmatiello.mockfn.matchers/make`
function. The required arguments are the matcher name, the matching function and
an expected value.

```clj
(mfn.m/make "equal to" = :expected)
```

Refer to the function's docstring for more details.

### Extra Features

The `me.pmatiello.mockfn.xtras` namespace provides additional mocking utilities.

```clj
(:require [me.pmatiello.mockfn.xtras :as xtras])
```

#### Returning different values at each invocation

The `return-in-order` function allows a mock to return a sequence of values in
the order the mock is called. After the last value, it continues from the first
value in the sequence.

Framework-agnostic usage:

```clj
(testing "returns values in order"
  (mfn/providing
    [(one-fn) (xtras/return-in-order :a :b :c)]
    (is (= [:a :b :c :a :b] (repeatedly 5 one-fn)))))
```

Using the `clojure.test` syntax sugar:

```clj
(mfn/deftest return-in-order-test
  (is (= [:a :b :c :a :b] (repeatedly 5 one-fn)))
  (mfn/providing
    (one-fn) (xtras/return-in-order :a :b :c)))
```

#### Verifying the order of interactions

The `calls-ordered?` function returns whether calls to mock functions occurred
in the specified order.

Framework-agnostic usage:

```clj
(testing "calls-ordered"
  (mfn/providing
    [(one-fn :x) :one
     (other-fn :y) :other]
    (one-fn :x) (other-fn :y)
    (is (xtras/calls-ordered? [#'one-fn :x] [#'other-fn :y]))))
```

Using the `clojure.test` syntax sugar:

```clj
(mfn/deftest calls-ordered-test
  (one-fn :x)
  (other-fn :y)
  (is (xtras/calls-ordered? [#'one-fn :x] [#'other-fn :y]))
  (mfn/providing
    (one-fn :x) :one
    (other-fn :y) :other))
```

The history of calls may be incomplete when mocked functions are called from
threads that do not inherit the current dynamic bindings, such as manually
created `Thread` instances.

#### Mocking protocol methods

The `reify-with` macro receives a protocol and a mapping of methods to
functions, producing an object that delegates calls to those functions for the
mapped protocol methods. Regular mocking primitives can be configured against
the functions to specify and validate interactions with the object.

```clj
(defprotocol SomeProtocol
  (m1 [this])
  (m2 [this x]))

(declare fn1 fn2)
(def obj (xtras/reify-with SomeProtocol {:m1 fn1 :m2 fn2}))
```

Framework-agnostic usage:

```clj
(mfn/providing
  [(fn1 obj) :fn1
   (fn2 obj :x) :fn2]
  (is (= :fn1 (.m1 obj)))
  (is (= :fn2 (.m2 obj :x))))
```

Using the `clojure.test` syntax sugar:

```clj
(mfn/deftest protocol-methods-test
  (is (= :fn1 (.m1 obj)))
  (is (= :fn2 (.m2 obj :x)))
  (mfn/providing
    (fn1 obj) :fn1
    (fn2 obj :x) :fn2))
```

## Quirks and Limitations

The aspects described below are considered expected behavior and unlikely to
change in future versions of mockfn.

### No support for parallel test execution

This library relies on the `clojure.core/with-redefs` macro to rebind function
vars to mock implementations. These redefinitions are temporary, but visible to
all threads while in effect. Therefore, test suites using mockfn must be
executed sequentially to avoid nondeterministic behavior.

### Inner definitions override outer ones

While `providing`, `verifying`, and `verifying-eventually` calls can be nested,
all required stubs and expectations for a single mock must be defined within a
single call. Mocking a function in an inner `providing`, `verifying`, or
`verifying-eventually` call will override any definitions made in the outer
scope for the tests being run in the inner scope.

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
file and the release version and date in the [changelog](./CHANGELOG.md).

Run all tests and build the release artifact:

```
% clj -X:test
% clj -T:build clean
% clj -T:build jar
% ls target/
```

Commit the changes and create a new tag:

```
% export VERSION=$(clj -A:build -M -e '(load-file "build.clj") (println build/version)' \
  | tail -n-1)
% git commit -a -m "Release: ${VERSION}"
% git tag -a "v${VERSION}" -m "Release: ${VERSION}"
% git push
% git push origin "v${VERSION}"
```

Publish to [Clojars](https://clojars.org):

```
% mvn deploy:deploy-file \
      -Dfile=target/mockfn-${VERSION}.jar \
      -DrepositoryId=clojars \
      -Durl=https://clojars.org/repo \
      -DpomFile=target/classes/META-INF/maven/me.pmatiello/mockfn/pom.xml \
      -DgeneratePom=false \
      -Daether.connector.basic.parallelPut=false
```

Note that this step requires Clojars to be configured as a server in the local
`~/.m2/settings.xml` file.

## Contribution Policy

This software is open-source, but closed to contributions.

## License

Distributed under the Eclipse Public License either version 2.0 or (at your
option) any later version.
