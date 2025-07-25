# Change Log

All notable changes to this project are documented in this file.

## Releases

## 0.9.0 - 2025-07-25

- Added support for matching a single matcher against all remaining call
  arguments.
- Added presence matchers: some, null.
- Improved documentation.

## 0.8.0 - 2025-07-07

- Added support for returning different return values at each mock invocation.

## 0.7.0 - 2025-04-25

- Added explicit support for mocks that invoke the original implementation.
- Added support for mocks that throw exceptions when invoked.

## 0.6.0 - 2025-04-14

- Added support for mocks that invoke functions instead of returning fixed
  values.
- Changed verify so that the evaluated body is returned it (as providing does).

## 0.5.0 - 2025-03-10

- Added support for mocking private functions.

## 0.4.0 - 2025-03-04

- Added collection matchers.
- Added boolean matchers.
- Added new numeric matcher: between.
- Added logical operators for matchers.
- Removed prefixes from matcher names.

## 0.3.0 - 2025-02-21

- Improved documentation formatting.
- Renamed `mockfn.macros` to `mockfn.plain`.
- Made matchers more readable and explicit in error messages.
- Added predicate matcher.

## 0.2.0 - 2025-02-18

- Added string matchers.
- Improved documentation.

## 0.1.0 - 2025-01-04

- Added function stubs.
- Added call verification.
- Added argument matchers.
- Added syntax sugar for `clojure.test`.
