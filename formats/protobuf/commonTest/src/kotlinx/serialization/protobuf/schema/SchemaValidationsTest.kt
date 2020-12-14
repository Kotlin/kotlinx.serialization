package kotlinx.serialization.protobuf.schema

import kotlinx.serialization.*
import kotlinx.serialization.protobuf.ProtoNumber
import kotlin.test.Test
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
        assertFailsWith(IllegalArgumentException::class) { generateProto2Schema(descriptors) }
    }

    @Test
    fun testInvalidClassSerialName() {
        val descriptors = listOf(InvalidClassName.serializer().descriptor)
        assertFailsWith(IllegalArgumentException::class) { generateProto2Schema(descriptors) }
    }

    @Test
    fun testInvalidClassFieldSerialName() {
        val descriptors = listOf(InvalidClassFieldName.serializer().descriptor)
        assertFailsWith(IllegalArgumentException::class) { generateProto2Schema(descriptors) }
    }

    @Test
    fun testDuplicateSerialNames() {
        val descriptors = listOf(InvalidClassFieldName.serializer().descriptor)
        assertFailsWith(IllegalArgumentException::class) { generateProto2Schema(descriptors) }
    }

    @Test
    fun testInvalidEnumSerialName() {
        val descriptors = listOf(InvalidEnumName.serializer().descriptor)
        assertFailsWith(IllegalArgumentException::class) { generateProto2Schema(descriptors) }
    }

    @Test
    fun testDuplicationSerialName() {
        val descriptors = listOf(ValidClass.serializer().descriptor, DuplicateClass.serializer().descriptor)
        assertFailsWith(IllegalArgumentException::class) { generateProto2Schema(descriptors) }
    }

    @Test
    fun testIllegalPackageNames() {
        val descriptors = listOf(ValidClass.serializer().descriptor)
        assertFailsWith(IllegalArgumentException::class) { generateProto2Schema(descriptors, "") }
        assertFailsWith(IllegalArgumentException::class) { generateProto2Schema(descriptors, ".") }
        assertFailsWith(IllegalArgumentException::class) { generateProto2Schema(descriptors, ".first.dot") }
        assertFailsWith(IllegalArgumentException::class) { generateProto2Schema(descriptors, "ended.with.dot.") }
        assertFailsWith(IllegalArgumentException::class) { generateProto2Schema(descriptors, "first._underscore") }
        assertFailsWith(IllegalArgumentException::class) { generateProto2Schema(descriptors, "first.1digit") }
        assertFailsWith(IllegalArgumentException::class) { generateProto2Schema(descriptors, "illegal.sym+bol") }
    }

    @Test
    fun testValidPackageNames() {
        val descriptors = listOf(ValidClass.serializer().descriptor)
        generateProto2Schema(descriptors, "singleIdent")
        generateProto2Schema(descriptors, "double.ident")
        generateProto2Schema(descriptors, "with.digits0123")
        generateProto2Schema(descriptors, "with.underscore_")
    }

    @Test
    fun testFieldNumberDuplicates() {
        assertFailsWith(IllegalArgumentException::class) { generateProto2Schema(listOf(FieldNumberDuplicates.serializer().descriptor)) }
        assertFailsWith(IllegalArgumentException::class) { generateProto2Schema(listOf(FieldNumberImplicitlyDuplicates.serializer().descriptor)) }
    }
}
