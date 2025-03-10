# Current developments

## Features
- Allow to inject a promotion hook to ExpectTests instead of a static boolean. This allows to, e.g. prompt the developer with a diff before promoting, or saving diff to files along test run.
- [Diff utilities](src/main/kotlin/nyub/expekt/diff) and [PromptWithPanel](src/main/kotlin/nyub/expekt/diff/PromptWithDiffPanel.kt) promotion trigger that displays a pop-up prompting to promote or reject a snapshot
- Allow to trigger promotion with a `promote@` label before expected string block (Kotlin only)
- Check or promote all expect calls within a test before raising assertion errors, with soft assertions
```kotlin
expectTest {
    "<CONTENT-1>".expect("""
    <OOPS-1>
    """) // Previously, test would have failed here

    // In promotion mode, it would have failed here because of an invalid single-quoted string
    "<CONTENT>".expect("<OOPS>")
    
    "<CONTENT-2>".expect("""
    <OOPS-2>
    """) // Now this will be checked or promoted too, then all errors are still raised
}
```

## Breaking Changes
- Promoted lines indentation is now based on the closing triple quotes indentation. This is breaking for Kotlin blocks not combined with `String#trimIndent()`.

# 1.0.1
## Bugfixes

- promotion could introduce unwanted empty lines on windows because of **CRLF** line endings. **NB**: promotion always introduce **LF**-separated lines, use git or your formatter to re-introduce **CRLF** if necessary.
- allow arbitrary spaces between expect and opening parenthesis
```kotlin
"<CONTENT>".expect   ("""
<CONTENT>
""")
```
- leading whitespaces were ignored in expected string block

Before the fix:
```kotlin
" <ACTUAL>".expect("""
 <ACTUAL>
""") // Assertion error "<ACTUAL>" is not equal to " <ACTUAL>"
```
- if there was content before the expected block closing quotes, it was kept as-is during promotion. Now enforce closing quote to be on a separate line.

Before the fix:
```kotlin
"<ACTUAL>".expect("""
<CONTENT>""")

// After promotion

"<ACTUAL>".expect("""
<ACTUAL>
<CONTENT>""") // Oops
```
This now raises an error "closing quotes must be on a different line than expected content"

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