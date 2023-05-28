# match-bits

`match-bits` is a macro that helps make masking and matching against specific patterns of bits much more ergonomic.

## Installation
TODO: Add rmckayfleming.match-bits to your dependencies.

## Usage

You can use the macro in a given namespace like so:

```
(ns your.namespace
  (:require [rmckayfleming.match-bits :refer [match-bits]]))
```

# Interface

`match-bits` works similarly to `case`. That is, it takes an expression that will be tested against a series of clauses and an optional fallthrough case.
```
(match-bits expr
  <pattern-1> <body-1>
  ...
  <pattern-n> <body-n>
  <fallthrough>?)
```

Patterns can be either exact integers or pattern symbols.

## Exact Integers

When given exact integers, match-bits works exactly the same as case.
```
(match-bits (+ 2r1000 2r0001)
  2r1000 :nope
  2r0001 :nope
  2r10000001 :nope
  2r1001 :yes!
  :default) => :yes!
```

## Pattern Symbols

Far more interesting are the use of pattern symbols. All pattern symbols must begin with `%`.

### Bit Masks

Any sequence of 0s and 1s will be matched exactly, similar to an exact integer.
```
(match-bits (+ 2r1000 2r0001)
  %1000 :nope
  %0001 :nope
  %10000001 :nope
  %1001 :yes!
  :default) => :yes!
```

However, unlike with an exact integer, any unspecified bits will be ignored!
```
(match-bits 2r10011010
  2r1010 :nope
  %1010 :yes!
  :default) => :yes!
```
Notice that the high bits 1001 are not tested against.

### Wildcards

If on the other hand you don't care about certain low-bits, you can replace them with `_`s:
```
(match-bits 2r10011010
  2r10010000 :nope
  %1001____ :yes!
  :default) => :yes!
```

### Pattern Variables

That's all and good, but what if you want to check the value of specific runs of bits? This is where pattern variables come in. Pattern variables are simply any run of the same alphabetic character in the pattern. The lexical environment of the corresponding body expression will be extended with symbols bound to the value of the specific bits. For instance:

```
(match-bits 2r10011010
  2r10010000 :nope
  %aaaabbbb [aaaa bbbb]
  :default) => [2r1001 2r1010]
```

```
(match-bits 2r10011010
  2r10010000 :nope
  %aaabbbbb [aaa bbbbb]
  :default) => [2r100 2r11010]
```

Note: a pattern variable can only appear once! i.e. `%aaabbaaa` is not a valid pattern symbol.

## Fallthrough

The fallthrough position lets you provide a default expression to evaluate:
```
(match-bits 2r10011010
  2r10010000 :nope
  (do (println "Default reached!")
    :yes!))
"Default reached!"
=> :yes!
```

If no fallthrough expression is provided, nil will be returned:
```
(match-bits 2r10011010
  2r10010000 :nope) => nil
```

# Contributing

`match-bits` is based on the library template from [seancorfield/deps-new](https://github.com/seancorfield/deps-new).

## Running Tests

To run the project's tests:

```
$ clojure -T:build test
```

## Running the CI

Run the project's CI pipeline and build a JAR (this will fail until you edit the tests to pass):

    $ clojure -T:build ci

This will produce an updated `pom.xml` file with synchronized dependencies inside the `META-INF`
directory inside `target/classes` and the JAR in `target`. You can update the version (and SCM tag)
information in generated `pom.xml` by updating `build.clj`.

Install it locally (requires the `ci` task be run first):

    $ clojure -T:build install

Deploy it to Clojars -- needs `CLOJARS_USERNAME` and `CLOJARS_PASSWORD` environment
variables (requires the `ci` task be run first):

    $ clojure -T:build deploy

Your library will be deployed to net.clojars.rmckayfleming/match-bits on clojars.org by default.
