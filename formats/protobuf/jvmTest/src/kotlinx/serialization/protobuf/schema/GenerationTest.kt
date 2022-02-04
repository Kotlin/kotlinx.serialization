package kotlinx.serialization.protobuf.schema

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.protobuf.ProtoIntegerType
import kotlinx.serialization.protobuf.*
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

internal const val TARGET_PACKAGE = "kotlinx.serialization.protobuf.schema.generator"
internal const val COMMON_SCHEMA_FILE_NAME = "common/schema.proto"
internal val commonClasses = listOf(
    GenerationTest.ScalarHolder::class,
    GenerationTest.FieldNumberClass::class,
    GenerationTest.SerialNameClass::class,
    GenerationTest.ListClass::class,
    GenerationTest.PackedListClass::class,
    GenerationTest.MapClass::class,
    GenerationTest.OptionalClass::class,
    GenerationTest.ContextualHolder::class,
    GenerationTest.AbstractHolder::class,
    GenerationTest.SealedHolder::class,
    GenerationTest.NestedCollections::class,
    GenerationTest.LegacyMapHolder::class,
    GenerationTest.NullableNestedCollections::class,
    GenerationTest.OptionalCollections::class,
)

class GenerationTest {
    @Serializable
    class ScalarHolder(
        val int: Int,
        @ProtoType(ProtoIntegerType.SIGNED)
        val intSigned: Int,
        @ProtoType(ProtoIntegerType.FIXED)
        val intFixed: Int,
        @ProtoType(ProtoIntegerType.DEFAULT)
        val intDefault: Int,

        val long: Long,
        @ProtoType(ProtoIntegerType.SIGNED)
        val longSigned: Long,
        @ProtoType(ProtoIntegerType.FIXED)
        val longFixed: Long,
        @ProtoType(ProtoIntegerType.DEFAULT)
        val longDefault: Int,

        val flag: Boolean,
        val byteArray: ByteArray,
        val boxedByteArray: Array<Byte?>,
        val text: String,
        val float: Float,
        val double: Double
    )

    @Serializable
    class FieldNumberClass(
        val a: Int,
        @ProtoNumber(5)
        val b: Int,
        @ProtoNumber(3)
        val c: Int
    )

    @Serializable
    @SerialName("OverriddenClassName")
    class SerialNameClass(
        val original: Int,
        @SerialName("OverriddenFieldName")
        val b: SerialNameEnum
    )

    @Serializable
    @SerialName("OverriddenEnumName")
    enum class SerialNameEnum {
        FIRST,

        @SerialName("OverriddenElementName")
        SECOND
    }

    @Serializable
    data class OptionsClass(val i: Int)

    @Serializable
    class ListClass(
        val intList: List<Int>,
        val intArray: IntArray,
        val boxedIntArray: Array<Int?>,
        val messageList: List<OptionsClass>,
        val enumList: List<SerialNameEnum>
    )

    @Serializable
    class PackedListClass(
        @ProtoPacked val intList: List<Int>,
        @ProtoPacked val intArray: IntArray,
        @ProtoPacked val boxedIntArray: Array<Int?>,
        val messageList: List<OptionsClass>,
        val enumList: List<SerialNameEnum>
    )

    @Serializable
    class MapClass(
        val scalarMap: Map<Int, Float>,
        val bytesMap: Map<Int, List<Byte>>,
        val messageMap: Map<String, OptionsClass>,
        val enumMap: Map<Boolean, SerialNameEnum>
    )

    @Serializable
    data class OptionalClass(
        val requiredInt: Int,
        val optionalInt: Int = 5,
        val nullableInt: Int?,
        val nullableOptionalInt: Int? = 10
    )

    @Serializable
    data class OptionalCollections(
        val requiredList: List<Int>,
        val optionalList: List<Int> = listOf(42),
        val nullableList: List<Int>?,
        val nullableOptionalList: List<Int>? = listOf(42),

        val requiredMap: Map<Int, Int>,
        val optionalMap: Map<Int, Int> = mapOf(42 to 42),
        val nullableMap: Map<Int, Int>?,
        val nullableOptionalMap: Map<Int, Int>? = mapOf(42 to 42)
    )

    @Serializable
    data class ContextualHolder(
        @Contextual val value: Int
    )

    @Serializable
    abstract class AbstractClass(val int: Int)

    @Serializable
    data class AbstractHolder(@Polymorphic val abs: AbstractClass)

    @Serializable
    sealed class SealedClass {
        @Serializable
        data class Impl1(val int: Int) : SealedClass()

        @Serializable
        data class Impl2(val long: Long) : SealedClass()
    }

    @Serializable
    data class SealedHolder(val sealed: SealedClass)

    @Serializable
    class NestedCollections(
        val intList: List<List<Int>>,
        val messageList: List<List<OptionsClass>>,
        val mapInList: List<Map<String, OptionsClass>>,
        val listInMap: Map<String, List<Int>>
    )

    @Serializable
    class LegacyMapHolder(
        val keyAsMessage: Map<OptionsClass, Int>,
        val keyAsEnum: Map<SerialNameEnum, OptionsClass>,
        val keyAsBytes: Map<List<Byte>, List<Byte>>,
        val keyAsList: Map<List<Int>, List<Byte>>,
        val keyAsDeepList: Map<List<List<Int>>, List<Byte>>,
        val nullableKeyAndValue: Map<OptionsClass?, OptionsClass?>
    )

    @Serializable
    class NullableNestedCollections(
        val nullableIntList: List<List<Int>?>,
        val nullableIntMap: Map<String, List<Int>?>,
        val intMap: Map<String, List<Int?>>,
        val intList: List<List<Int?>>,
        val legacyMap: Map<List<Int>?, List<Int>?>
    )

    @Test
    fun testIndividuals() {
        assertSchemaForClass(OptionsClass::class, mapOf("java_package" to "api.proto", "java_outer_classname" to "Outer"))
        commonClasses.forEach {
            assertSchemaForClass(it)
        }
    }

    @Test
    fun testCommon() {
        assertSchema(COMMON_SCHEMA_FILE_NAME, commonClasses.map { it.serializer().descriptor }.toList())
    }

    private fun assertSchemaForClass(
        clazz: KClass<*>,
        options: Map<String, String> = emptyMap()
    ) {
        assertSchema("${clazz.simpleName}.proto", listOf(clazz.serializer().descriptor), options)
    }

    private fun assertSchema(
        fileName: String,
        descriptors: List<SerialDescriptor>,
        options: Map<String, String> = emptyMap()
    ) {
        val schema = this::class.java.getResourceAsStream("/$fileName")
            .readBytes().toString(Charsets.UTF_8)
            .replace("\r\n", "\n") // fixme when compiled on windows, the file contains line \r\n breaks
        assertEquals(schema, ProtoBufSchemaGenerator.generateSchemaText(descriptors, TARGET_PACKAGE, options))
    }
}
