# JSON parser and syntax tree for Kotlin/Native

Since plugin API unavailable for K/N compiler yet,
kotlinx.serialization offers separate common+native artifacts for parsing JSON into tree-like structure
with minimalistic API:

```kotlin
import kotlinx.serialziation.json.*

val input = """{"a": "foo", "b": 10, "c": true, "d": null}"""
val elem: JsonElement = JsonTreeParser(input).read() // or .readFully() to throw exception if input was not consumed fully

elem as JsonObject
elem.keys == setOf("a", "b", "c", "d") // true
assertEquals(JsonLiteral("foo"), elem["a"])
println(elem.getAsValue("b")?.asInt) // 10
```

Whole JSON tree sources and API can be found [here](common/src/kotlinx/serialization/json/JsonAst.kt#L22).

## Installing

On JVM and JS, this module included into artifacts of kotlinx.serialization, so JSON tree available 'as is', including common code.
If you're using native, you can't use `kotlinx-serialization-runtime-common`, since it contains more features.
You need to declare dependency only on `kotlinx-serialziation-runtime-jsonparser` in your common code, and then use
`org.jetbrains.kotlinx:jsonparser-native` dependency in Native. Example of native dependency can be found [here](../example-native) (CLI application)
or [here](../example-native) (iOS application alongside technology preview of HTTP client for native).
 
