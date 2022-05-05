package kotlinx.serialization.protobuf.schema

import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith

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
}
