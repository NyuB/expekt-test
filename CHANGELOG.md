# Last developments
## Breaking changes
- To allow to restrict triple quoted string search range, `expect(actual, expected)` is now an extension function `actual.expect(expected)`.

## Bugfixes
- non-trailing or non-leading empty lines were removed from the actual content (leading and trailing empty lines are still removed)
- an erroneous call to `expect("...")` without triple quoted string block would replace the next block in the file instead of raising an error
```kotlin
print("Not within triple quotes")
expect("Not within triple quotes") // not within tripe quotes

print("Do not confuse this block with the missing one above")
expect("""
    Do not confuse this block with the missing one above
    """.trimIndent())
```

Note that, since the search is based on the triple quote sequence that could be on the line after the expect call, the pathological example below can still trigger this problem:
```kotlin
print("Not within triple quotes")
expect("Not within triple quotes") // not within tripe quotes
"""
This will be considered as the triple quote for the expect call above
""".trimIndent()
```

# 0.0.1
Very first release

## Features
- Core expekt-test library for inline snapshot testing
- `ExpectTests#expectTest { }` scope function for Kotlin
- `ExpectTestExtension` JUnit extension for java