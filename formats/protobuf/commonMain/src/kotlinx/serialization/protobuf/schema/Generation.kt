@file:OptIn(ExperimentalSerializationApi::class)

package kotlinx.serialization.protobuf.schema

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.protobuf.ProtoIntegerType
import kotlinx.serialization.protobuf.ProtoNumber
import kotlinx.serialization.protobuf.ProtoType
import kotlin.native.concurrent.SharedImmutable

/**
 * Generate protocol buffers version 2 schema text for given serializable descriptors.
 * All descriptors will be placed in one schema which can be saved in one `.proto` file.
 * @param descriptors descriptors of serializable classes for which the scheme will be generated
 * @param packageName protobuf common package for schema
 * @param options protobuf options to include in schema
 */
@ExperimentalSerializationApi
public fun generateProto2Schema(
    descriptors: List<SerialDescriptor>,
    packageName: String? = null,
    options: Map<String, String> = emptyMap()
): String {
    packageName?.let { if (!it.isProtobufFullIdent) throw IllegalArgumentException("Incorrect protobuf package name '$it'") }
    checkDoubles(descriptors)
    return generateProto2SchemaString(descriptors, packageName, options)
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
        throw IllegalArgumentException("Serial names of types are duplicated $duplicates")
    }
}

private fun generateProto2SchemaString(
    descriptors: List<SerialDescriptor>,
    packageName: String?,
    options: Map<String, String>
): String {
    val builder = StringBuilder()
    builder.appendLine("""syntax = "proto2";""")
        .appendLine()

    packageName?.let {
        builder.append("package ").append(it).appendLine(';')
    }
    for ((optionName, optionValue) in options) {
        builder.append("option ").append(optionName).append(" = \"").append(optionValue).appendLine("\";")
    }

    val generatedTypes = mutableSetOf<String>()
    val queue = ArrayDeque<SerialDescriptor>()
    queue.addAll(descriptors)

    while (queue.isNotEmpty()) {
        builder.appendLine()
        val descriptor = queue.removeFirst()
        val name = descriptor.messageOrEnumName
        if (generatedTypes.contains(name)) {
            continue
        }
        queue.addAll(generateType(descriptor, builder))
        generatedTypes += name
    }

    return builder.toString()
}


internal val SyntheticPolymorphicDescriptor: SerialDescriptor =
    buildClassSerialDescriptor("KotlinxSerializationPolymorphic") {
        element("type", PrimitiveSerialDescriptor("typeDescriptor", PrimitiveKind.STRING))
        element("value", buildSerialDescriptor("valueDescriptor", StructureKind.LIST) {
            element<Byte>("0")
        })
    }

private fun generateType(descriptor: SerialDescriptor, builder: StringBuilder): List<SerialDescriptor> {
    return when {
        descriptor.isProtobufMessage -> generateMessage(descriptor, builder)
        descriptor.isProtobufEnum -> {
            generateEnum(descriptor, builder)
            emptyList()
        }
        else -> throw IllegalStateException(
            "Unrecognized custom type with serial name "
                    + "'${descriptor.serialName}' and kind '${descriptor.kind}'"
        )
    }
}

private fun generateMessage(
    messageDescriptor: SerialDescriptor,
    builder: StringBuilder
): List<SerialDescriptor> {
    builder.append("// serial name '").append(removeLineBreaks(messageDescriptor.serialName)).appendLine('\'')

    val messageName = messageDescriptor.messageOrEnumName
    if (!messageName.isProtobufIdent) {
        throw IllegalArgumentException("Invalid name for the message in protobuf scheme '$messageName'. Serial name of the class '${messageDescriptor.serialName}'")
    }

    builder.append("message ").append(messageName).appendLine(" {")

    val usedNumbers: MutableSet<Int> = mutableSetOf()
    val nestedTypes = mutableListOf<SerialDescriptor>()
    for (index in 0 until messageDescriptor.elementsCount) {
        val fieldName = messageDescriptor.getElementName(index)
        if (!fieldName.isProtobufIdent) {
            throw IllegalArgumentException("Invalid name of the field in protobuf scheme '$messageName' for class with serial name '${messageDescriptor.serialName}'")
        }

        val fieldDescriptor = messageDescriptor.getElementDescriptor(index)

        nestedTypes += when {
            fieldDescriptor.isProtobufNamedType -> generateNamedType(messageDescriptor, index, builder)
            fieldDescriptor.isProtobufRepeated -> generateListType(messageDescriptor, index, builder)
            fieldDescriptor.isProtobufMap -> generateMapType(messageDescriptor, index, builder)
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

        builder.append(' ')
        builder.append(fieldName)
        builder.append(" = ")
        builder.append(number)
        builder.appendLine(';')
    }
    builder.appendLine('}')

    return nestedTypes
}

private fun generateNamedType(
    messageDescriptor: SerialDescriptor,
    index: Int,
    builder: StringBuilder
): List<SerialDescriptor> {
    val fieldDescriptor = messageDescriptor.getElementDescriptor(index)
    val nestedTypes: List<SerialDescriptor>
    val typeName: String = when {
        messageDescriptor.isSealedPolymorphic && index == 1 -> {
            builder.appendLine("  // decoded as message with one of these types:")
            nestedTypes = fieldDescriptor.elementDescriptors.toList()
            nestedTypes.forEachIndexed { _, childDescriptor ->
                builder.append("  //   message ").append(childDescriptor.messageOrEnumName)
                    .append(", serial name '").append(removeLineBreaks(childDescriptor.serialName)).appendLine('\'')
            }
            fieldDescriptor.scalarTypeName()
        }
        fieldDescriptor.isProtobufScalar -> {
            nestedTypes = emptyList()
            fieldDescriptor.scalarTypeName(messageDescriptor.getElementAnnotations(index))
        }
        fieldDescriptor.isOpenPolymorphic -> {
            nestedTypes = listOf(SyntheticPolymorphicDescriptor)
            SyntheticPolymorphicDescriptor.serialName
        }
        else -> {
            // enum or regular message
            nestedTypes = listOf(fieldDescriptor)
            fieldDescriptor.messageOrEnumName
        }
    }

    if (fieldDescriptor.isNullable && messageDescriptor.isElementOptional(index)) {
        builder.appendLine("  // WARNING: this field is nullable and has default value, it's impossible to unambiguously interpret an absence value.")
        builder.appendLine("  //   For this field null value does not support and absence value denotes as default value. Default value is not present in the schema.")
    }
    if (!fieldDescriptor.isNullable && messageDescriptor.isElementOptional(index)) {
        builder.appendLine("  // WARNING: an absence value is decoded as a default value that is not present in the schema")
    }
    val optional = fieldDescriptor.isNullable || messageDescriptor.isElementOptional(index)

    builder.append("  ")
        .append(if (optional) "optional " else "required ")
        .append(typeName)

    return nestedTypes
}

private fun generateMapType(
    messageDescriptor: SerialDescriptor,
    index: Int,
    builder: StringBuilder
): List<SerialDescriptor> {
    val mapDescriptor = messageDescriptor.getElementDescriptor(index)
    val fieldName = messageDescriptor.getElementName(index)
    val valueDescriptor = mapDescriptor.getElementDescriptor(1).let {
        if (it.isProtobufCollection) {
            if (it.isNullable) {
                builder.appendLine("  // WARNING: null value is not supported for nested collections")
            }
            val wrapperName = "${messageDescriptor.messageOrEnumName}_${fieldName}"
            buildClassSerialDescriptor(wrapperName) {
                element("value", it)
            }
        } else {
            if (it.isNullable) {
                builder.appendLine("  // WARNING: null value is not supported for map value")
            }
            it
        }
    }

    if (!mapDescriptor.isNullable && messageDescriptor.isElementOptional(index)) {
        builder.appendLine("  // WARNING: This field does not support empty map")
        builder.appendLine("  //  An absence value is decoded as a default value that is not present in the schema")
    } else if (mapDescriptor.isNullable && !messageDescriptor.isElementOptional(index)) {
        builder.appendLine("  // WARNING: This field is marked as nullable but it does not support null values")
    } else if (mapDescriptor.isNullable && messageDescriptor.isElementOptional(index)) {
        builder.appendLine("  // WARNING: This field is marked as nullable and has a default value but it does not support null values and empty map")
        builder.appendLine("  //  An absence value is decoded as a default value that is not present in the schema")
    }

    builder.append("  map<")
    builder.append(mapDescriptor.getElementDescriptor(0).scalarTypeName(mapDescriptor.getElementAnnotations(0)))
    builder.append(", ")
    builder.append(valueDescriptor.protobufTypeName(mapDescriptor.getElementAnnotations(1)))
    builder.append(">")

    return if (valueDescriptor.isProtobufMessageOrEnum) {
        listOf(valueDescriptor)
    } else {
        emptyList()
    }
}

private fun generateListType(
    messageDescriptor: SerialDescriptor,
    index: Int,
    builder: StringBuilder
): List<SerialDescriptor> {
    val collectionDescriptor = messageDescriptor.getElementDescriptor(index)
    val fieldName = messageDescriptor.getElementName(index)

    val elementDescriptor = if (collectionDescriptor.kind == StructureKind.LIST) {
        collectionDescriptor.getElementDescriptor(0).let {
            if (it.isProtobufCollection) {
                if (it.isNullable) {
                    builder.appendLine("  // WARNING: null value is not supported for nested collections")
                }
                val wrapperName = "${messageDescriptor.messageOrEnumName}_${fieldName}"
                buildClassSerialDescriptor(wrapperName) {
                    element("value", it)
                }
            } else {
                if (it.isNullable) {
                    builder.appendLine("  // WARNING: null value is not supported for list elements")
                }
                it
            }
        }
    } else {
        val wrapperName = "${messageDescriptor.messageOrEnumName}_${fieldName}"
        buildClassSerialDescriptor(wrapperName) {
            element("key", collectionDescriptor.getElementDescriptor(0))
            element("value", collectionDescriptor.getElementDescriptor(1))
        }
    }

    if (!collectionDescriptor.isNullable && messageDescriptor.isElementOptional(index)) {
        builder.appendLine("  // WARNING: This field does not support empty list")
        builder.appendLine("  //  An absence value is decoded as a default value that is not present in the schema")
    } else if (collectionDescriptor.isNullable && !messageDescriptor.isElementOptional(index)) {
        builder.appendLine("  // WARNING: This field is marked as nullable but it does not support null values")
    } else if (collectionDescriptor.isNullable && messageDescriptor.isElementOptional(index)) {
        builder.appendLine("  // WARNING: This field is marked as nullable and has a default value but it does not support null values and empty list")
        builder.appendLine("  //  An absence value is decoded as a default value that is not present in the schema")
    }

    val typeName = elementDescriptor.protobufTypeName(messageDescriptor.getElementAnnotations(index))
    builder.append("  repeated ").append(typeName)

    return if (elementDescriptor.isProtobufMessageOrEnum) {
        listOf(elementDescriptor)
    } else {
        emptyList()
    }
}

private fun generateEnum(
    enum: SerialDescriptor,
    builder: StringBuilder
) {
    val enumName = enum.messageOrEnumName
    if (!enumName.isProtobufIdent) {
        throw IllegalArgumentException("Invalid name for the message in protobuf scheme '$enumName'. Serial name of the class '${enum.serialName}'")
    }
    builder.append("// serial name '").append(enum.serialName).appendLine('\'')
    builder.append("enum ").append(enumName).appendLine(" {")

    enum.elementDescriptors.forEachIndexed { number, element ->
        val elementName = element.protobufEnumElementName
        if (!elementName.isProtobufIdent) {
            throw IllegalArgumentException("The element name '$elementName' is invalid in the protobuf schema")
        }
        builder.append("  ").append(elementName).append(" = ").append(number).appendLine(';')
    }
    builder.appendLine('}')
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
    get() = serialName.substringAfterLast('.', serialName)

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

private fun removeLineBreaks(text: String): String {
    return text.replace('\n', ' ').replace('\r', ' ')
}

@SharedImmutable
private val IDENT_REGEX = Regex("[A-Za-z][A-Za-z0-9_]*")

private val String.isProtobufFullIdent: Boolean
    get() = split('.').all { it.isProtobufIdent }

private val String.isProtobufIdent: Boolean get() = matches(IDENT_REGEX)
