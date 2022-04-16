@file:OptIn(ExperimentalSerializationApi::class)

package kotlinx.serialization.protobuf.schema

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.protobuf.*
import kotlinx.serialization.protobuf.internal.*

/**
 * Experimental generator of ProtoBuf schema that is compatible with [serializable][Serializable] Kotlin classes
 * and data encoded and decoded by [ProtoBuf] format.
 *
 * The schema is generated based on provided [SerialDescriptor] and is compatible with proto2 schema definition.
 * An arbitrary Kotlin class represent much wider object domain than the ProtoBuf specification, thus schema generator
 * has the following list of restrictions:
 *
 *  * Serial name of the class and all its fields should be a valid Proto [identifier](https://developers.google.com/protocol-buffers/docs/reference/proto2-spec)
 *  * Nullable values are allowed only for Kotlin [nullable][SerialDescriptor.isNullable] types, but not [optional][SerialDescriptor.isElementOptional]
 *    in order to properly distinguish "default" and "absent" values.
 *  * The name of the type without the package directive uniquely identifies the proto message type and two or more fields with the same serial name
 *    are considered to have the same type. Schema generator allows to specify a separate package directive for the pack of classes in order
 *    to mitigate this limitation.
 *  * Nested collections, e.g. `List<List<Int>>` are represented using the artificial wrapper message in order to distinguish
 *    repeated fields boundaries.
 *  * Default Kotlin values are not representable in proto schema. A special commentary is generated for properties with default values.
 *  * Empty nullable collections are not supported by the generated schema and will be prohibited in [ProtoBuf] as well
 *    due to their ambiguous nature.
 *
 * Temporary restrictions:
 *  * [Contextual] data is represented as as `bytes` type
 *  * [Polymorphic] data is represented as a artificial `KotlinxSerializationPolymorphic` message.
 *
 * Other types are mapped according to their specification: primitives as primitives, lists as 'repeated' fields and
 * maps as 'repeated' map entries.
 *
 * The name of messages and enums is extracted from [SerialDescriptor.serialName] in [SerialDescriptor] without the package directive,
 * as substring after the last dot character, the `'?'` character is also removed if it is present at the end of the string.
 */
@ExperimentalSerializationApi
public object ProtoBufSchemaGenerator {

    /**
     * Generate text of protocol buffers schema version 2 for the given [rootDescriptor].
     * The resulting schema will contain all types referred by [rootDescriptor].
     *
     * [packageName] define common protobuf package for all messages and enum in the schema, it may contain `'a'`..`'z'`
     * letters in upper and lower case, decimal digits, `'.'` or `'_'` chars, but must be started only by a letter and
     * not finished by a dot.
     *
     * [options] define values for protobuf options. Option value (map value) is an any string, option name (map key)
     * should be the same format as [packageName].
     *
     * The method throws [IllegalArgumentException] if any of the restrictions imposed by [ProtoBufSchemaGenerator] is violated.
     */
    @ExperimentalSerializationApi
    public fun generateSchemaText(
        rootDescriptor: SerialDescriptor,
        packageName: String? = null,
        options: Map<String, String> = emptyMap()
    ): String = generateSchemaText(listOf(rootDescriptor), packageName, options)

    /**
     * Generate text of protocol buffers schema version 2 for the given serializable [descriptors].
     * [packageName] define common protobuf package for all messages and enum in the schema, it may contain `'a'`..`'z'`
     * letters in upper and lower case, decimal digits, `'.'` or `'_'` chars, but started only from a letter and
     * not finished by dot.
     *
     * [options] define values for protobuf options. Option value (map value) is an any string, option name (map key)
     * should be the same format as [packageName].
     *
     * The method throws [IllegalArgumentException] if any of the restrictions imposed by [ProtoBufSchemaGenerator] is violated.
     */
    @ExperimentalSerializationApi
    public fun generateSchemaText(
        descriptors: List<SerialDescriptor>,
        packageName: String? = null,
        options: Map<String, String> = emptyMap()
    ): String {
        packageName?.let { p -> p.checkIsValidFullIdentifier { "Incorrect protobuf package name '$it'" } }
        checkDoubles(descriptors)
        val builder = StringBuilder()
        builder.generateProto2SchemaText(descriptors, packageName, options)
        return builder.toString()
    }

    private fun checkDoubles(descriptors: List<SerialDescriptor>) {
        val rootTypesNames = mutableSetOf<String>()
        val duplicates = mutableListOf<String>()

        descriptors.map { it.messageOrEnumName }.forEach {
            if (!rootTypesNames.add(it)) {
                duplicates += it
            }
        }
        if (duplicates.isNotEmpty()) {
            throw IllegalArgumentException("Serial names of the following types are duplicated: $duplicates")
        }
    }

    private fun StringBuilder.generateProto2SchemaText(
        descriptors: List<SerialDescriptor>,
        packageName: String?,
        options: Map<String, String>
    ) {
        appendLine("""syntax = "proto2";""").appendLine()

        packageName?.let { append("package ").append(it).appendLine(';') }

        for ((optionName, optionValue) in options) {
            val safeOptionName = removeLineBreaks(optionName)
            val safeOptionValue = removeLineBreaks(optionValue)
            safeOptionName.checkIsValidFullIdentifier { "Invalid option name '$it'" }
            append("option ").append(safeOptionName).append(" = \"").append(safeOptionValue).appendLine("\";")
        }

        val generatedTypes = mutableSetOf<String>()
        val queue = ArrayDeque<TypeDefinition>()
        descriptors.map { TypeDefinition(it) }.forEach { queue.add(it) }

        while (queue.isNotEmpty()) {
            val type = queue.removeFirst()
            val descriptor = type.descriptor
            val name = descriptor.messageOrEnumName
            if (!generatedTypes.add(name)) {
                continue
            }

            appendLine()
            when {
                descriptor.isProtobufMessage -> queue.addAll(generateMessage(type))
                descriptor.isProtobufEnum -> generateEnum(type)
                else -> throw IllegalStateException(
                    "Unrecognized custom type with serial name "
                            + "'${descriptor.serialName}' and kind '${descriptor.kind}'"
                )
            }
        }
    }

    private fun StringBuilder.generateMessage(messageType: TypeDefinition): List<TypeDefinition> {
        val messageDescriptor = messageType.descriptor
        val messageName: String
        if (messageType.isSynthetic) {
            append("// This message was generated to support ").append(messageType.ability)
                .appendLine(" and does not present in Kotlin.")

            messageName = messageDescriptor.serialName
            if (messageType.containingMessageName != null) {
                append("// Containing message '").append(messageType.containingMessageName).append("', field '")
                    .append(messageType.fieldName).appendLine('\'')
            }
        } else {
            messageName = messageDescriptor.messageOrEnumName
            messageName.checkIsValidIdentifier {
                "Invalid name for the message in protobuf schema '$messageName'. " +
                        "Serial name of the class '${messageDescriptor.serialName}'"
            }
            val safeSerialName = removeLineBreaks(messageDescriptor.serialName)
            if (safeSerialName != messageName) {
                append("// serial name '").append(safeSerialName).appendLine('\'')
            }
        }

        append("message ").append(messageName).appendLine(" {")

        val usedNumbers: MutableSet<Int> = mutableSetOf()
        val nestedTypes = mutableListOf<TypeDefinition>()
        for (index in 0 until messageDescriptor.elementsCount) {
            val fieldName = messageDescriptor.getElementName(index)
            fieldName.checkIsValidIdentifier {
                "Invalid name of the field '$fieldName' in message '$messageName' for class with serial " +
                        "name '${messageDescriptor.serialName}'"
            }

            val fieldDescriptor = messageDescriptor.getElementDescriptor(index)

            val isList = fieldDescriptor.isProtobufRepeated

            nestedTypes += when {
                fieldDescriptor.isProtobufNamedType -> generateNamedType(messageType, index)
                isList -> generateListType(messageType, index)
                fieldDescriptor.isProtobufMap -> generateMapType(messageType, index)
                else -> throw IllegalStateException(
                    "Unprocessed message field type with serial name " +
                            "'${fieldDescriptor.serialName}' and kind '${fieldDescriptor.kind}'"
                )
            }


            val annotations = messageDescriptor.getElementAnnotations(index)
            val number = annotations.filterIsInstance<ProtoNumber>().singleOrNull()?.number ?: index + 1
            if (!usedNumbers.add(number)) {
                throw IllegalArgumentException("Field number $number is repeated in the class with serial name ${messageDescriptor.serialName}")
            }

            append(' ').append(fieldName).append(" = ").append(number)

            val isPackRequested = annotations.filterIsInstance<ProtoPacked>().singleOrNull() != null

            when {
                !isPackRequested ||
                !isList || // ignore as packed only meaningful on repeated types
                !fieldDescriptor.getElementDescriptor(0).isPackable // Ignore if the type is not allowed to be packed
                     -> appendLine(';')
                else -> appendLine(" [packed=true];")
            }
        }
        appendLine('}')

        return nestedTypes
    }

    private fun StringBuilder.generateNamedType(messageType: TypeDefinition, index: Int): List<TypeDefinition> {
        val messageDescriptor = messageType.descriptor

        val fieldDescriptor = messageDescriptor.getElementDescriptor(index)
        val nestedTypes: List<TypeDefinition>
        val typeName: String = when {
            messageDescriptor.isSealedPolymorphic && index == 1 -> {
                appendLine("  // decoded as message with one of these types:")
                nestedTypes = fieldDescriptor.elementDescriptors.map { TypeDefinition(it) }.toList()
                nestedTypes.forEachIndexed { _, childType ->
                    append("  //   message ").append(childType.descriptor.messageOrEnumName).append(", serial name '")
                        .append(removeLineBreaks(childType.descriptor.serialName)).appendLine('\'')
                }
                fieldDescriptor.scalarTypeName()
            }
            fieldDescriptor.isProtobufScalar -> {
                nestedTypes = emptyList()
                fieldDescriptor.scalarTypeName(messageDescriptor.getElementAnnotations(index))
            }
            fieldDescriptor.isOpenPolymorphic -> {
                nestedTypes = listOf(SyntheticPolymorphicType)
                SyntheticPolymorphicType.descriptor.serialName
            }
            else -> {
                // enum or regular message
                nestedTypes = listOf(TypeDefinition(fieldDescriptor))
                fieldDescriptor.messageOrEnumName
            }
        }

        if (messageDescriptor.isElementOptional(index)) {
            appendLine("  // WARNING: a default value decoded when value is missing")
        }
        val optional = fieldDescriptor.isNullable || messageDescriptor.isElementOptional(index)

        append("  ").append(if (optional) "optional " else "required ").append(typeName)

        return nestedTypes
    }

    private fun StringBuilder.generateMapType(messageType: TypeDefinition, index: Int): List<TypeDefinition> {
        val messageDescriptor = messageType.descriptor
        val mapDescriptor = messageDescriptor.getElementDescriptor(index)
        val originalMapValueDescriptor = mapDescriptor.getElementDescriptor(1)
        val valueType = if (originalMapValueDescriptor.isProtobufCollection) {
            createNestedCollectionType(messageType, index, originalMapValueDescriptor, "nested collection in map value")
        } else {
            TypeDefinition(originalMapValueDescriptor)
        }
        val valueDescriptor = valueType.descriptor

        if (originalMapValueDescriptor.isNullable) {
            appendLine("  // WARNING: nullable map values can not be represented in protobuf")
        }
        generateCollectionAbsenceComment(messageDescriptor, mapDescriptor, index)

        val keyTypeName = mapDescriptor.getElementDescriptor(0).scalarTypeName(mapDescriptor.getElementAnnotations(0))
        val valueTypeName = valueDescriptor.protobufTypeName(mapDescriptor.getElementAnnotations(1))
        append("  map<").append(keyTypeName).append(", ").append(valueTypeName).append(">")

        return if (valueDescriptor.isProtobufMessageOrEnum) {
            listOf(valueType)
        } else {
            emptyList()
        }
    }

    private fun StringBuilder.generateListType(messageType: TypeDefinition, index: Int): List<TypeDefinition> {
        val messageDescriptor = messageType.descriptor
        val collectionDescriptor = messageDescriptor.getElementDescriptor(index)
        val originalElementDescriptor = collectionDescriptor.getElementDescriptor(0)
        val elementType = if (collectionDescriptor.kind == StructureKind.LIST) {
            if (originalElementDescriptor.isProtobufCollection) {
                createNestedCollectionType(messageType, index, originalElementDescriptor, "nested collection in list")
            } else {
                TypeDefinition(originalElementDescriptor)
            }
        } else {
            createLegacyMapType(messageType, index, "legacy map")
        }

        val elementDescriptor = elementType.descriptor

        if (elementDescriptor.isNullable) {
            appendLine("  // WARNING: nullable elements of collections can not be represented in protobuf")
        }
        generateCollectionAbsenceComment(messageDescriptor, collectionDescriptor, index)

        val typeName = elementDescriptor.protobufTypeName(messageDescriptor.getElementAnnotations(index))
        append("  repeated ").append(typeName)

        return if (elementDescriptor.isProtobufMessageOrEnum) {
            listOf(elementType)
        } else {
            emptyList()
        }
    }

    private fun StringBuilder.generateEnum(enumType: TypeDefinition) {
        val enumDescriptor = enumType.descriptor
        val enumName = enumDescriptor.messageOrEnumName
        enumName.checkIsValidIdentifier {
            "Invalid name for the enum in protobuf schema '$enumName'. Serial name of the enum " +
                    "class '${enumDescriptor.serialName}'"
        }
        val safeSerialName = removeLineBreaks(enumDescriptor.serialName)
        if (safeSerialName != enumName) {
            append("// serial name '").append(enumName).appendLine('\'')
        }

        append("enum ").append(enumName).appendLine(" {")

        enumDescriptor.elementDescriptors.forEachIndexed { number, element ->
            val elementName = element.protobufEnumElementName
            elementName.checkIsValidIdentifier {
                "The enum element name '$elementName' is invalid in the " +
                        "protobuf schema. Serial name of the enum class '${enumDescriptor.serialName}'"
            }
            append("  ").append(elementName).append(" = ").append(number).appendLine(';')
        }
        appendLine('}')
    }

    private val SerialDescriptor.isOpenPolymorphic: Boolean
        get() = kind == PolymorphicKind.OPEN

    private val SerialDescriptor.isSealedPolymorphic: Boolean
        get() = kind == PolymorphicKind.SEALED

    private val SerialDescriptor.isProtobufNamedType: Boolean
        get() = isProtobufMessageOrEnum || isProtobufScalar

    private val SerialDescriptor.isProtobufScalar: Boolean
        get() = (kind is PrimitiveKind)
                || (kind is StructureKind.LIST && getElementDescriptor(0).kind === PrimitiveKind.BYTE)
                || kind == SerialKind.CONTEXTUAL

    private val SerialDescriptor.isProtobufMessageOrEnum: Boolean
        get() = isProtobufMessage || isProtobufEnum

    private val SerialDescriptor.isProtobufMessage: Boolean
        get() = kind == StructureKind.CLASS || kind == StructureKind.OBJECT || kind == PolymorphicKind.SEALED || kind == PolymorphicKind.OPEN

    private val SerialDescriptor.isProtobufCollection: Boolean
        get() = isProtobufRepeated || isProtobufMap

    private val SerialDescriptor.isProtobufRepeated: Boolean
        get() = (kind == StructureKind.LIST && getElementDescriptor(0).kind != PrimitiveKind.BYTE)
                || (kind == StructureKind.MAP && !getElementDescriptor(0).isValidMapKey)

    private val SerialDescriptor.isProtobufMap: Boolean
        get() = kind == StructureKind.MAP && getElementDescriptor(0).isValidMapKey

    private val SerialDescriptor.isProtobufEnum: Boolean
        get() = kind == SerialKind.ENUM

    private val SerialDescriptor.isValidMapKey: Boolean
        get() = kind == PrimitiveKind.INT || kind == PrimitiveKind.LONG || kind == PrimitiveKind.BOOLEAN || kind == PrimitiveKind.STRING


    private val SerialDescriptor.messageOrEnumName: String
        get() = (serialName.substringAfterLast('.', serialName)).removeSuffix("?")

    private fun SerialDescriptor.protobufTypeName(annotations: List<Annotation> = emptyList()): String {
        return if (isProtobufScalar) {
            scalarTypeName(annotations)
        } else {
            messageOrEnumName
        }
    }

    private val SerialDescriptor.protobufEnumElementName: String
        get() = serialName.substringAfterLast('.', serialName)

    private fun SerialDescriptor.scalarTypeName(annotations: List<Annotation> = emptyList()): String {
        val integerType = annotations.filterIsInstance<ProtoType>().firstOrNull()?.type ?: ProtoIntegerType.DEFAULT

        if (kind == SerialKind.CONTEXTUAL) {
            return "bytes"
        }

        if (kind is StructureKind.LIST && getElementDescriptor(0).kind == PrimitiveKind.BYTE) {
            return "bytes"
        }

        return when (kind as PrimitiveKind) {
            PrimitiveKind.BOOLEAN -> "bool"
            PrimitiveKind.BYTE, PrimitiveKind.CHAR, PrimitiveKind.SHORT, PrimitiveKind.INT ->
                when (integerType) {
                    ProtoIntegerType.DEFAULT -> "int32"
                    ProtoIntegerType.SIGNED -> "sint32"
                    ProtoIntegerType.FIXED -> "fixed32"
                }
            PrimitiveKind.LONG ->
                when (integerType) {
                    ProtoIntegerType.DEFAULT -> "int64"
                    ProtoIntegerType.SIGNED -> "sint64"
                    ProtoIntegerType.FIXED -> "fixed64"
                }
            PrimitiveKind.FLOAT -> "float"
            PrimitiveKind.DOUBLE -> "double"
            PrimitiveKind.STRING -> "string"
        }
    }

    private data class TypeDefinition(
        val descriptor: SerialDescriptor,
        val isSynthetic: Boolean = false,
        val ability: String? = null,
        val containingMessageName: String? = null,
        val fieldName: String? = null
    )

    private val SyntheticPolymorphicType = TypeDefinition(
        buildClassSerialDescriptor("KotlinxSerializationPolymorphic") {
            element("type", PrimitiveSerialDescriptor("typeDescriptor", PrimitiveKind.STRING))
            element("value", buildSerialDescriptor("valueDescriptor", StructureKind.LIST) {
                element("0", Byte.serializer().descriptor)
            })
        },
        true,
        "polymorphic types"
    )

    private class NotNullSerialDescriptor(val original: SerialDescriptor) : SerialDescriptor by original {
        override val isNullable = false
    }

    private val SerialDescriptor.notNull get() = NotNullSerialDescriptor(this)

    private fun StringBuilder.generateCollectionAbsenceComment(
        messageDescriptor: SerialDescriptor,
        collectionDescriptor: SerialDescriptor,
        index: Int
    ) {
        if (!collectionDescriptor.isNullable && messageDescriptor.isElementOptional(index)) {
            appendLine("  // WARNING: a default value decoded when value is missing")
        } else if (collectionDescriptor.isNullable && !messageDescriptor.isElementOptional(index)) {
            appendLine("  // WARNING: an empty collection decoded when a value is missing")
        } else if (collectionDescriptor.isNullable && messageDescriptor.isElementOptional(index)) {
            appendLine("  // WARNING: a default value decoded when value is missing")
        }
    }

    private fun createLegacyMapType(
        messageType: TypeDefinition,
        index: Int,
        description: String
    ): TypeDefinition {
        val messageDescriptor = messageType.descriptor
        val fieldDescriptor = messageDescriptor.getElementDescriptor(index)
        val fieldName = messageDescriptor.getElementName(index)
        val messageName = messageDescriptor.messageOrEnumName

        val wrapperName = "${messageName}_${fieldName}"
        val wrapperDescriptor = buildClassSerialDescriptor(wrapperName) {
            element("key", fieldDescriptor.getElementDescriptor(0).notNull)
            element("value", fieldDescriptor.getElementDescriptor(1).notNull)
        }

        return TypeDefinition(
            wrapperDescriptor,
            true,
            description,
            messageType.containingMessageName ?: messageName,
            messageType.fieldName ?: fieldName
        )
    }

    private fun createNestedCollectionType(
        messageType: TypeDefinition,
        index: Int,
        elementDescriptor: SerialDescriptor,
        description: String
    ): TypeDefinition {
        val messageDescriptor = messageType.descriptor
        val fieldName = messageDescriptor.getElementName(index)
        val messageName = messageDescriptor.messageOrEnumName

        val wrapperName = "${messageName}_${fieldName}"
        val wrapperDescriptor = buildClassSerialDescriptor(wrapperName) {
            element("value", elementDescriptor.notNull)
        }

        return TypeDefinition(
            wrapperDescriptor,
            true,
            description,
            messageType.containingMessageName ?: messageName,
            messageType.fieldName ?: fieldName
        )
    }

    private fun removeLineBreaks(text: String): String {
        return text.replace('\n', ' ').replace('\r', ' ')
    }

    private val IDENTIFIER_REGEX = Regex("[A-Za-z][A-Za-z0-9_]*")

    private fun String.checkIsValidFullIdentifier(messageSupplier: (String) -> String) {
        if (split('.').any { !it.matches(IDENTIFIER_REGEX) }) {
            throw IllegalArgumentException(messageSupplier.invoke(this))
        }
    }

    private fun String.checkIsValidIdentifier(messageSupplier: () -> String) {
        if (!matches(IDENTIFIER_REGEX)) {
            throw IllegalArgumentException(messageSupplier.invoke())
        }
    }
}
