package kotlinx.serialization.protobuf.schema

import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*
import kotlin.jvm.*
import kotlin.test.*

class SchemaValidationsTest {
    @Serializable
    data class ValidClass(val i: Int)

    @Serializable
    @SerialName("ValidClass")
    data class DuplicateClass(val l: Long)

    @Serializable
    @SerialName("invalid serial name")
    data class InvalidClassName(val i: Int)

    @Serializable
    data class InvalidClassFieldName(@SerialName("invalid serial name") val i: Int)

    @Serializable
    data class FieldNumberDuplicates(@ProtoNumber(42) val i: Int, @ProtoNumber(42) val j: Int)

    @Serializable
    data class FieldNumberImplicitlyDuplicates(@ProtoNumber(2) val i: Int, val j: Int)

    @Serializable
    @SerialName("invalid serial name")
    enum class InvalidEnumName { SINGLETON }

    @Serializable
    enum class InvalidEnumElementName {
        FIRST,

        @SerialName("invalid serial name")
        SECOND
    }

    @Serializable
    enum class EnumWithExplicitProtoNumberDuplicate {
        @ProtoNumber(2)
        FIRST,
        @ProtoNumber(2)
        SECOND,
    }

    @Serializable
    enum class EnumWithImplicitProtoNumberDuplicate {
        FIRST,
        @ProtoNumber(0)
        SECOND,
    }

    @Test
    fun testExplicitDuplicateEnumElementProtoNumber() {
        val descriptors = listOf(EnumWithExplicitProtoNumberDuplicate.serializer().descriptor)
        assertFailsWith(IllegalArgumentException::class) { ProtoBufSchemaGenerator.generateSchemaText(descriptors) }
    }

    @Test
    fun testImplicitDuplicateEnumElementProtoNumber() {
        val descriptors = listOf(EnumWithImplicitProtoNumberDuplicate.serializer().descriptor)
        assertFailsWith(IllegalArgumentException::class) { ProtoBufSchemaGenerator.generateSchemaText(descriptors) }
    }

    @Test
    fun testInvalidEnumElementSerialName() {
        val descriptors = listOf(InvalidEnumElementName.serializer().descriptor)
        assertFailsWith(IllegalArgumentException::class) { ProtoBufSchemaGenerator.generateSchemaText(descriptors) }
    }

    @Test
    fun testInvalidClassSerialName() {
        val descriptors = listOf(InvalidClassName.serializer().descriptor)
        assertFailsWith(IllegalArgumentException::class) { ProtoBufSchemaGenerator.generateSchemaText(descriptors) }
    }

    @Test
    fun testInvalidClassFieldSerialName() {
        val descriptors = listOf(InvalidClassFieldName.serializer().descriptor)
        assertFailsWith(IllegalArgumentException::class) { ProtoBufSchemaGenerator.generateSchemaText(descriptors) }
    }

    @Test
    fun testDuplicateSerialNames() {
        val descriptors = listOf(InvalidClassFieldName.serializer().descriptor)
        assertFailsWith(IllegalArgumentException::class) { ProtoBufSchemaGenerator.generateSchemaText(descriptors) }
    }

    @Test
    fun testInvalidEnumSerialName() {
        val descriptors = listOf(InvalidEnumName.serializer().descriptor)
        assertFailsWith(IllegalArgumentException::class) { ProtoBufSchemaGenerator.generateSchemaText(descriptors) }
    }

    @Test
    fun testDuplicationSerialName() {
        val descriptors = listOf(ValidClass.serializer().descriptor, DuplicateClass.serializer().descriptor)
        assertFailsWith(IllegalArgumentException::class) { ProtoBufSchemaGenerator.generateSchemaText(descriptors) }
    }

    @Test
    fun testInvalidOptionName() {
        val descriptors = listOf(ValidClass.serializer().descriptor)
        assertFailsWith(IllegalArgumentException::class) {
            ProtoBufSchemaGenerator.generateSchemaText(
                descriptors,
                options = mapOf("broken name" to "value")
            )
        }
    }

    @Test
    fun testIllegalPackageNames() {
        val descriptors = listOf(ValidClass.serializer().descriptor)
        assertFailsWith(IllegalArgumentException::class) { ProtoBufSchemaGenerator.generateSchemaText(descriptors, "") }
        assertFailsWith(IllegalArgumentException::class) { ProtoBufSchemaGenerator.generateSchemaText(descriptors, ".") }
        assertFailsWith(IllegalArgumentException::class) { ProtoBufSchemaGenerator.generateSchemaText(descriptors, ".first.dot") }
        assertFailsWith(IllegalArgumentException::class) { ProtoBufSchemaGenerator.generateSchemaText(descriptors, "ended.with.dot.") }
        assertFailsWith(IllegalArgumentException::class) { ProtoBufSchemaGenerator.generateSchemaText(descriptors, "first._underscore") }
        assertFailsWith(IllegalArgumentException::class) { ProtoBufSchemaGenerator.generateSchemaText(descriptors, "first.1digit") }
        assertFailsWith(IllegalArgumentException::class) { ProtoBufSchemaGenerator.generateSchemaText(descriptors, "illegal.sym+bol") }
    }

    @Test
    fun testValidPackageNames() {
        val descriptors = listOf(ValidClass.serializer().descriptor)
        ProtoBufSchemaGenerator.generateSchemaText(descriptors, "singleIdent")
        ProtoBufSchemaGenerator.generateSchemaText(descriptors, "double.ident")
        ProtoBufSchemaGenerator.generateSchemaText(descriptors, "with.digits0123")
        ProtoBufSchemaGenerator.generateSchemaText(descriptors, "with.underscore_")
    }

    @Test
    fun testFieldNumberDuplicates() {
        assertFailsWith(IllegalArgumentException::class) { ProtoBufSchemaGenerator.generateSchemaText(listOf(FieldNumberDuplicates.serializer().descriptor)) }
        assertFailsWith(IllegalArgumentException::class) { ProtoBufSchemaGenerator.generateSchemaText(listOf(FieldNumberImplicitlyDuplicates.serializer().descriptor)) }
    }

    @Serializable
    data class OneOfData(
        @ProtoNumber(1) val name: String,
        @ProtoOneOf val i: IType
    )

    @Serializable
    sealed interface IType

    @Serializable
    data class IntType(@ProtoNumber(2) val intValue: Int): IType

    @Serializable
    @JvmInline
    value class StringType(@ProtoNumber(3) val strValue: String): IType

    @Serializable
    data class WrapType(@ProtoNumber(4) val content: InnerType): IType

    @Serializable
    data class InnerType(val innerContent: String)

    @Test
    fun testOneOfGenerate() {
        val descriptors = listOf(OneOfData.serializer().descriptor)
        ProtoBufSchemaGenerator.generateSchemaText(descriptors).also {
            println(it)
            assertContains(it, "oneof i")
            assertContains(it, "message InnerType")
            // oneof fields need no required keyword
            assertFalse(it.contains("required int32"))
        }

        assertFailsWithMessage<IllegalArgumentException>(
            message = "Implementation of oneOf type kotlinx.serialization.protobuf.ProtobufOneOfTest.FailType should contain only 1 element, but get 2"
        ) {
            ProtoBufSchemaGenerator.generateSchemaText(ProtobufOneOfTest.FailOuter.serializer().descriptor)
        }
    }
}
