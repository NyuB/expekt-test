# Current developments
## Features
- Allow arbitrary spaces between expect and opening parenthesis

# 1.0.0
## Features

- Widen supported Java version, produce Java 15 bytecode instead of 17
- Improved error messages precision for invalid expected string blocks
- Removed constraint on expected string block starting line, it can now be anywhere after the `expect(` call

## Bugfixes

- promotion failure when promoting content in parallel within the same file. Promotion is now synchronized on the ExpectTests class.
- promotion failure when `expect(` was written within the expected string block on the same line as the actual `expect(` call.
- promotion failure when using string interpolation within expected string block

# 0.1.0
Mostly bug fixes related to erroneous expect("...") calls without proper triple-quoted block formatting.

## Breaking changes
- To allow to restrict triple quoted string search range, `expect(actual, expected)` is now an extension function `actual.expect(expected)`.

## Bugfixes
- non-trailing or non-leading empty lines were removed from the actual content (leading and trailing empty lines are still removed)
- an erroneous call to `expect("...")` without triple quoted string block would replace the next block in the file instead of raising an error
```kotlin
expect("Not within triple quotes") // not within tripe quotes
expect("""
    Do not confuse this block with the missing one above
    """.trimIndent())
```

# 0.0.1
Very first release

## Features
- Core expekt-test library for inline snapshot testing
- `ExpectTests#expectTest { }` scope function for Kotlin
- `ExpectTestExtension` JUnit extension for java