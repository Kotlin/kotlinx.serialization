/*
 * Copyright 2017-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.properties

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*

/**
 * Transforms a [Serializable] class' properties into a single flat [String] representing the class data
 * in the properties format.
 *
 * If the given class has non-primitive property `d` of arbitrary type `D`, `D` values are inserted
 * into the same map; keys for such values are prefixed with string `d.`:
 *
 * ```
 * @Serializable
 * class Data(val property1: String)
 *
 * @Serializable
 * class DataHolder(val data: Data, val property2: String)
 *
 * val string = StringProperties.encodeToString(properties)
 * // string contents will be the following:
 * """
 *     property2 = value2
 *     data.property1 = value1
 * """
 * ```
 *
 * If the given class has a [List] property `l`, each value from the list
 * would be prefixed with `l.N.`, where N is an index for a particular value.
 * [Map] is treated as a `[key,value,...]` list.

 * Conversely, this class can convert a properties string into a [Serializable] class instance.
 * ```
 * @Serializable
 * class Data(val property1: String)
 *
 * @Serializable
 * class DataHolder(val data: Data, val property2: String)
 *
 * val string = """
 *     property2 = value2
 *     data.property1 = value1
 * """
 * val data = StringProperties.decodeToString(string, DataHolder.serializer())
 * // data contents will be the following:
 * // DataHolder(data = Data(property1 = "value1"), property2 = "value2")
 * ```
 *
 * @param conf A [PropertiesConf] which contain configuration for customising the output string.
 */
@ExperimentalSerializationApi
@Suppress("UNUSED_PARAMETER")
public sealed class StringProperties(
    private val conf: PropertiesConf,
    private val properties: Properties = Properties(conf.serializersModule),
) : SerialFormat by properties, StringFormat {

    /**
     * Encodes properties from the given [value] to a properties String using the given [serializer].
     * `null` values are omitted from the output.
     */
    @ExperimentalSerializationApi
    public override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
        val map = properties.encodeToMap(serializer, value)
        val builder = StringBuilder()
        for ((k, v) in map) {
            builder.append(k)
            repeat(conf.spacesBeforeSeparator) {
                builder.append(' ')
            }
            builder.append(conf.keyValueSeparator.char())
            repeat(conf.spacesAfterSeparator) {
                builder.append(' ')
            }
            builder.append(v)
            builder.append(conf.lineSeparator.chars())
        }
        return builder.toString()
    }

    /**
     * Decodes properties from the given [string] to a value of type [T] using the given [deserializer].
     * [String] values are converted to respective primitive types using default conversion methods.
     * [T] may contain properties of nullable types; they will be filled by non-null values from the [map], if present.
     */
    public override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T {
        val result = mutableMapOf<String, String>()
        for (line in string.logicalLines()) {
            val parsedLine = line.unescaped()
            var keyEnd = parsedLine.length
            for (i in parsedLine.indices) {
                if (parsedLine[i] in separators) {
                    keyEnd = i
                    break
                }
            }

            var valueBegin = parsedLine.length
            var separatorFound = false
            for (i in keyEnd..parsedLine.lastIndex) {
                if (separatorFound && parsedLine[i] != ' ') {
                    valueBegin = i
                    break
                }
                if (parsedLine[i] in nonBlankSeparators) {
                    separatorFound = true
                }
                if (parsedLine[i] !in separators) {
                    valueBegin = i
                    break
                }
            }

            result[parsedLine.substring(0, keyEnd)] = parsedLine.substring(valueBegin)
        }
        return properties.decodeFromStringMap(deserializer, result)
    }

    /**
     * A [Properties] instance that can be used as default and does not have any [SerializersModule] installed.
     */
    @ExperimentalSerializationApi
    public companion object Default : StringProperties(PropertiesConf())
}

@OptIn(ExperimentalSerializationApi::class)
private class StringPropertiesImpl(conf: PropertiesConf) : StringProperties(conf)

/**
 * Creates an instance of [StringProperties] with a given [builderAction].
 * TODO: doc
 */
@ExperimentalSerializationApi
public fun StringProperties(builderAction: StringPropertiesBuilder.() -> Unit = {}): StringProperties {
    val builder = StringPropertiesBuilder(PropertiesConf())
    builder.builderAction()
    return StringPropertiesImpl(builder.build())
}

/**
 * Encodes properties from given [value] to a string using serializer for reified type [T] and returns this string.
 * Converts all primitive types to [String] using [toString] method.
 * `null` values are omitted from the output.
 */
@ExperimentalSerializationApi
public inline fun <reified T> StringProperties.encodeToString(value: T): String =
    encodeToString(serializersModule.serializer(), value)

/**
 * Decodes properties from given [propertiesString], assigns them to an object using serializer for reified type [T] and returns this object.
 * [String] values are converted to respective primitive types using default conversion methods.
 * [T] may contain properties of nullable types; they will be filled by non-null values from the [map], if present.
 */
@ExperimentalSerializationApi
public inline fun <reified T> StringProperties.decodeFromString(propertiesString: String): T =
    decodeFromString(serializersModule.serializer(), propertiesString)

/**
 * Builder of the [StringProperties] instance provided by `StringProperties { ... }` factory function.
 */
@ExperimentalSerializationApi
public class StringPropertiesBuilder internal constructor(from: PropertiesConf) {

    /**
     * A [LineSeparator] to be used for separating lines when encoding to a string.
     * Default value is [LineSeparator.LF].
     */
    public var lineSeparator: LineSeparator = from.lineSeparator

    /**
     * A [KeyValueSeparator] to be used for separating keys and values when encoding to a string.
     * Default value is [KeyValueSeparator.EQUALS].
     */
    public var keyValueSeparator: KeyValueSeparator = from.keyValueSeparator

    /**
     * A number of spaces to be inserted before the [keyValueSeparator] when encoding to a string.
     * Default value is `0`.
     */
    public var spacesBeforeSeparator: Int = from.spacesBeforeSeparator

    /**
     * A number of spaces to be inserted after the [keyValueSeparator] when encoding to a string.
     * Default value is `0`.
     */
    public var spacesAfterSeparator: Int = from.spacesAfterSeparator

    /**
     * A [SerializersModule] to be used for encoding and decoding.
     * Default value is [EmptySerializersModule].
     */
    public var module: SerializersModule = from.serializersModule

    internal fun build(): PropertiesConf {
        return PropertiesConf(
            lineSeparator,
            keyValueSeparator,
            spacesBeforeSeparator,
            spacesAfterSeparator,
            module
        )
    }
}

@ExperimentalSerializationApi
internal data class PropertiesConf(
    val lineSeparator: LineSeparator = LineSeparator.LF,
    val keyValueSeparator: KeyValueSeparator = KeyValueSeparator.EQUALS,
    val spacesBeforeSeparator: Int = 0,
    val spacesAfterSeparator: Int = 0,
    val serializersModule: SerializersModule = EmptySerializersModule()
)

@ExperimentalSerializationApi
public enum class LineSeparator(private val s: String) {
    LF("\n"),
    CR("\r"),
    CRLF("\r\n");

    public fun chars(): CharArray {
        return s.toCharArray()
    }
}

@ExperimentalSerializationApi
public enum class KeyValueSeparator(private val c: Char) {
    EQUALS('='),
    COLON(':');

    public fun char(): Char = c
}

private val nonBlankSeparators = setOf('=', ':')
private val separators = nonBlankSeparators + ' '
private val wellKnownEscapeChars = mapOf(
    '\\' to '\\',
    'n' to '\n',
    'r' to '\r',
    't' to '\t'
)

private fun String.unescaped(): String {
    val sb = StringBuilder(this.length)
    var i = 0
    while (i < this.length) {
        if (i < this.length - 1 && this[i] == '\\') {
            if (this[i + 1] in wellKnownEscapeChars) {
                sb.append(wellKnownEscapeChars[this[i + 1]])
                i += 2
            } else {
                i++
            }
        } else {
            sb.append(this[i])
            i++
        }
    }
    return sb.toString()
}

private fun String.logicalLines(): List<String> {
    val commentFilter = "[ \\t\\f]*[#!].*".toRegex()
    val lines = lines()
        .filterNot { it.isBlank() || commentFilter.matches(it) }
        .toMutableList()
    val logicalLines = mutableListOf<String>()

    var currentLine = ""
    for (line in lines) {
        val trimmedLine = line.trimStart()
        if (trimmedLine.endsWith("\\")) {
            currentLine += trimmedLine.dropLast(1)
        } else {
            currentLine += trimmedLine
            logicalLines.add(currentLine)
            currentLine = ""
        }
    }
    if (currentLine.isNotBlank()) {
        logicalLines.add(currentLine)
    }

    return logicalLines
}
