/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.builtins.LongAsStringSerializer
import kotlinx.serialization.internal.DynamicObjectParser
import kotlinx.serialization.internal.DynamicObjectSerializer
import kotlinx.serialization.json.*

/**
 * Converts native JavaScript objects into Kotlin ones, verifying their types.
 *
 * A result of `decodeFromDynamic(nativeObj)` should be the same as
 * `kotlinx.serialization.json.Json.decodeFromString(kotlin.js.JSON.stringify(nativeObj))`.
 * This class also supports array-based polymorphism if the corresponding flag in [Json.configuration] is set to `true`.
 * Does not support any other [Map] keys than [String].
 * Has limitation on [Long] type: any JS number that is greater than
 * [`abs(2^53-1)`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Number/MAX_SAFE_INTEGER)
 * is considered to be imprecise and therefore can't be deserialized to [Long]. Either use [Double] type
 * for such values or pass them as strings using [LongAsStringSerializer] afterwards.
 *
 * Usage example:
 *
 * ```
 * @Serializable
 * data class Data(val a: Int)
 *
 * @Serializable
 * data class DataWrapper(val s: String, val d: Data?)
 *
 * val dyn: dynamic = js("""{s:"foo", d:{a:42}}""")
 * val parsed = Json.decodeFromDynamic(DataWrapper.serializer(), dyn)
 * parsed == DataWrapper("foo", Data(42)) // true
 * ```
 */
@ExperimentalSerializationApi
public fun <T> Json.decodeFromDynamic(deserializer: DeserializationStrategy<T>, dynamic: dynamic): T {
    return DynamicObjectParser(serializersModule, configuration).parse(dynamic, deserializer)
}

/**
 * A reified version of [decodeFromDynamic].
 */
@ExperimentalSerializationApi
public inline fun <reified T> Json.decodeFromDynamic(dynamic: dynamic): T =
    decodeFromDynamic(serializersModule.serializer(), dynamic)

/**
 * Converts Kotlin data structures to plain Javascript objects
 *
 * Limitations:
 * * Map keys must be of primitive or enum type
 * * Currently does not support polymorphism
 * * All [Long] values must be less than [`abs(2^53-1)`](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Number/MAX_SAFE_INTEGER).
 * Otherwise, they're encoded as doubles with precision loss and require `isLenient` flag of [Json.configuration] set to true.
 *
 * Example of usage:
 * ```
 *  @Serializable
 *  open class DataWrapper(open val s: String, val d: String?)
 *
 *  val wrapper = DataWrapper("foo", "bar")
 *  val plainJS: dynamic = Json.encodeToDynamic(DataWrapper.serializer(), wrapper)
 * ```
 */
@ExperimentalSerializationApi
public fun <T> Json.encodeToDynamic(serializer: SerializationStrategy<T>, value: T): dynamic {
    return DynamicObjectSerializer(serializersModule, configuration, false).serialize(serializer, value)
}

/**
 * A reified version of [encodeToDynamic].
 */
@ExperimentalSerializationApi
public inline fun <reified T> Json.encodeToDynamic(value: T): dynamic =
    encodeToDynamic(serializersModule.serializer(), value)
