![ci-badge](https://github.com/NyuB/expekt-test/actions/workflows/ci.yml/badge.svg?branch=main)
![latest release](https://img.shields.io/github/release/NyuB/expekt-test)
![latest release date](https://img.shields.io/github/release-date/NyuB/expekt-test)

# Expekt tests

Inline snapshot testing for Kotlin and Java, inspired by JaneStreet's [expect-tests framework for OCaml](https://blog.janestreet.com/the-joy-of-expect-tests/)

![waveforms demo](doc/waveforms.gif)

- [Principles](#principles)
- [Alternatives and external resources on snapshot testing](#alternatives-and-external-resources-on-snapshot-testing)
- [Setup](#setup)
  + [Maven](#from-maven)
  + [From Source](#from-source)
- [Usage](#usage)
  + [Kotlin](#kotlin)
  + [Java](#java)
  + [Examples](#examples)
  + [Constraints on expected string blocks](#constraints-on-expected-string-blocks)

## Principles

Expekt is a lightweight **inline** **snapshot testing** tool:
- targeted toward to the assert/then part of a typical arrange-act-assert/given-when-then test setup
- compare textual representation of expected (**snapshot**ed) outputs and actual ones. With Expekt, the expected outputs are expressed as raw string blocks in source code (the **inline** part).
- offers a 'promotion' mechanism to replace the expected outputs with the actual ones when the user (developer) decides the change is suitable. The promotion is done directly in the source code, altering the string blocks. 

This means that efforts to represent your objects as readable strings, (such as ascii art, tables, graphs ...) can be directly exploited as readable assertions.
It also makes updating these assertions effortless, should their representation change.

## Alternatives and external resources on snapshot testing

Snapshot testing (also called approval, acceptance, golden-master, ...) is a popular technique in many ecosystems. 

A few resources on this topic:
- [The JaneStreet article on the expect-test library that inspired Expekt](https://blog.janestreet.com/the-joy-of-expect-tests/)
- [An article describing the advantages of inline snapshot testing](https://ianthehenry.com/posts/my-kind-of-repl/)
- [An article from TigerBeetle describing their Zig library for inline snapshot testing](https://tigerbeetle.com/blog/2024-05-14-snapshot-testing-for-the-masses/)

Some alternatives or equivalents in other ecosystems:
- [Selfie (Java/Python/JavaScript)](https://selfie.dev/jvm)
- [ApprovalTests (Java and many others)](https://github.com/approvals/approvaltests.java)
- [expect-test (OCaml)](https://github.com/janestreet/ppx_expect)
- [Jest (JavaScript)](https://jestjs.io/fr/docs/snapshot-testing)
- [Insta (Rust)](https://insta.rs/)
- [Verify (C#)](https://github.com/VerifyTests/Verify)

Here is a quick comparison table if you want to choose between Expekt and another JVM tool, or come from another language and want an idea of the corresponding features in Expekt:

| Tool          |        JVM         |    Inline snapshot | File snapshot      | Update control                                                                         | Interactive snapshot review | Extensible diff/review |
|---------------|:------------------:|-------------------:|--------------------|----------------------------------------------------------------------------------------|-----------------------------|------------------------|
| Expekt        | :white_check_mark: | :white_check_mark: | :x:                | user-configurable flag, per test class, test method, or `promote@` label on a snapshot | :x:                         | :x:                    | 
| Selfie        | :white_check_mark: | :white_check_mark: | :white_check_mark: | global flag, `toBe_TODO()` or `//selfieonce` comment                                   | :x:                         | :x:                    |
| ApprovalTests | :white_check_mark: |                :x: | :white_check_mark: | interactive, global flag                                                               | :white_check_mark:          | :white_check_mark:     |
| expect-test   |        :x:         | :white_check_mark: | :x:                | promotion command to update differing snapshots                                        | :x:                         | :x:                    |
| Jest          |        :x:         | :white_check_mark: | :white_check_mark: | interactive, promotion command to update differing snapshots                           | :white_check_mark:          | :x:                    |
| Insta         |        :x:         | :white_check_mark: | :white_check_mark: | interactive, global flag                                                               | :white_check_mark:          | :x:                    |
| Verify        |        :x:         |                :x: | :white_check_mark: | interactive, global flag                                                               | :white_check_mark:          | :white_check_mark:     |

## Setup

### From Maven

Add expekt dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.nyub</groupId>
    <artifactId>expekt-test</artifactId>
    <version>1.0.1</version>
    <scope>test</scope>
</dependency>
```

### From source

The source code is only comprised of 2 files (and even 1 if you don't use the JUnit extension).
Feel free to download [ExpectTests.kt](src/main/kotlin/nyub/expekt/ExpectTests.kt) and [ExpectTestExtension.kt](src/main/kotlin/nyub/expekt/junit/ExpectTestExtension.kt) directly into your sources and experiment.

## Usage

### Kotlin

The recommended usage for Kotlin is to define an ExpectTests shared configuration and write your test with the `expectTest { }` scope function. See [the kotlin tests](src/test/kotlin/nyub/expekt/KotlinUsageTest.kt) for setup examples.

Promotion is triggered:
- By passing `true` to the [ExpectTests](src/main/kotlin/nyub/expekt/ExpectTests.kt) `promote` parameter
- By adding a `promote@` label before the [expected string block](#constraints-on-expected-string-blocks). This overrides the above-mentioned `promote` parameter
```kotlin
"<CONTENT>".expect(promote@"""
<CONTENT>
""")
```

The JUnit 5 extension is also usable from the Kotlin side, even if it brings fewer improvements than for [Java users](#java).

### Java

The recommended usage for Java is to use the provided JUnit 5 extension. See [the java tests](src/test/kotlin/nyub/expekt/JavaUsageTest.java) for examples.

Promotion is triggered:
- By setting the system property `nyub.expekt.promote` to `"true"`
- At class level by using `@Promote(true/false)` annotation
- At method (test) level by using `@Promote(true/false)` annotation

For non-junit codebases, the Kotlin scope functions are usable on the Java side, with slightly degraded ergonomics.

### Examples

More examples are available in [the demo folder](src/test/kotlin/nyub/expekt/demos)

### Constraints on expected string blocks

Expekt detects string blocks to check and promote based on a few heuristics.

This imposes some formatting rule regarding the `expect(` call.

1) the `expect(` call should be written in place, not aliased
2) the expected content
   - should be in a triple-quoted-string block
   - should not use [Kotlin string interpolation](https://kotlinlang.org/docs/strings.html#string-templates)
3) the opening triple-quotes should be the next token after the `expect(` call (not nested in parentheses or aliased)
4) the closing triple-quotes
    - should be on a different line than the opening triple-quotes
    - should be on a different line than the expected content

#### OK:

```kotlin
expect("""
<CONTENT>
""")
```

```kotlin
expect(
"""
<CONTENT>
""")
```

```kotlin
expect(
"""
<CONTENT>
"""
)
```

#### Not OK:

```kotlin
expect("<CONTENT>")
```

- (because the expected content is not within a triple-quoted block)

```kotlin
expect("""<CONTENT>""")
```
- (because the closing quotes are on the same line as the opening quotes)

```kotlin
expect("""
<CONTENT>""")
```
- (because the closing quotes are on the same line as the expected content)

```kotlin
expect(f("""
<CONTENT>
"""))
```

- (because the opening quotes are not the next token after the `expect(` call)

```kotlin
fun alias(s: String) = expect(s)
alias(
"""
<CONTENT>
"""
)
```

- (because expect is aliased, so the search starts from the actual call site on the first line)

```kotlin
val content = "<CONTENT>"
expect("""
$content
""")
```

- (because the expected string block uses string interpolation)

See [ExpectTestsTest.ExpectCallConstraintsTest](src/test/kotlin/nyub/expekt/ExpectCallConstraintsTest.kt) for more invalid examples