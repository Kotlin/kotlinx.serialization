/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.test.*
import kotlin.reflect.*
import kotlin.test.*

@Suppress("RemoveExplicitTypeArguments") // This is exactly what's being tested
class SerializersLookupTest : JsonTestBase() {

    @Test
    fun testPrimitive() {
        val token = typeOf<Int>()
        val serial = serializer(token)
        assertSame(Int.serializer() as KSerializer<*>, serial)
        assertSerializedWithType("42", 42)
    }

    @Test
    fun testPlainClass() {
        val b = StringData("some string")
        assertSerializedWithType("""{"data":"some string"}""", b)
    }

    @Test
    fun testListWithT() {
        val source = """[{"intV":42}]"""
        val serial = serializer<List<IntData>>()
        assertEquals(listOf(IntData(42)), Json.decodeFromString(serial, source))
    }

    @Test
    fun testPrimitiveList() {
        val myArr = listOf("a", "b", "c")
        assertSerializedWithType("""["a","b","c"]""", myArr)
    }

    @Test
    fun testListAsCollection() {
        val myArr: Collection<String> = listOf("a", "b", "c")
        assertSerializedWithType("""["a","b","c"]""", myArr)
    }


    @Test
    fun testPrimitiveSet() {
        val mySet = setOf("a", "b", "c", "c")
        assertSerializedWithType("""["a","b","c"]""", mySet)
    }

    @Test
    fun testMapWithT() {
        val myMap = mapOf("string" to StringData("foo"), "string2" to StringData("bar"))
        assertSerializedWithType("""{"string":{"data":"foo"},"string2":{"data":"bar"}}""", myMap)
    }

    @Test
    fun testNestedLists() {
        val myList = listOf(listOf(listOf(1, 2, 3)), listOf())
        assertSerializedWithType("[[[1,2,3]],[]]", myList)
    }

    @Test
    fun testListSubtype() {
        val myList = arrayListOf(1, 2, 3)
        assertSerializedWithType<ArrayList<Int>>("[1,2,3]", myList)
        assertSerializedWithType<List<Int>>("[1,2,3]", myList)
    }

    @Test
    fun testListProjection() {
        val myList = arrayListOf(1, 2, 3)
        assertSerializedWithType<List<Int>>("[1,2,3]", myList)
        assertSerializedWithType<MutableList<out Int>>("[1,2,3]", myList)
        assertSerializedWithType<ArrayList<in Int>>("[1,2,3]", myList)
    }

    @Test
    fun testNullableTypes() {
        val myList: List<Int?> = listOf(1, null, 3)
        assertSerializedWithType("[1,null,3]", myList)
        assertSerializedWithType<List<Int?>?>("[1,null,3]", myList)
    }

    @Test
    fun testPair() {
        val myPair = "42" to 42
        assertSerializedWithType("""{"first":"42","second":42}""", myPair)
    }

    @Test
    fun testTriple() = noLegacyJs { // because of Box
        val myTriple = Triple("1", 2, Box(42))
        assertSerializedWithType("""{"first":"1","second":2,"third":{"boxed":42}}""", myTriple)
    }

    @Test
    fun testCustomGeneric() = noLegacyJs {
        val intBox = Box(42)
        val intBoxSerializer = serializer<Box<Int>>()
        assertEquals(Box.serializer(Int.serializer()).descriptor, intBoxSerializer.descriptor)
        assertSerializedWithType("""{"boxed":42}""", intBox)
        val dataBox = Box(StringData("foo"))
        assertSerializedWithType("""{"boxed":{"data":"foo"}}""", dataBox)
    }

    @Test
    fun testRecursiveGeneric() = noLegacyJs {
        val boxBox = Box(Box(Box(IntData(42))))
        assertSerializedWithType("""{"boxed":{"boxed":{"boxed":{"intV":42}}}}""", boxBox)
    }

    @Test
    fun testMixedGeneric() = noLegacyJs {
        val listOfBoxes = listOf(Box("foo"), Box("bar"))
        assertSerializedWithType("""[{"boxed":"foo"},{"boxed":"bar"}]""", listOfBoxes)
        val boxedList = Box(listOf("foo", "bar"))
        assertSerializedWithType("""{"boxed":["foo","bar"]}""", boxedList)
    }

    @Test
    fun testReferenceArrays() {
        assertSerializedWithType("[1,2,3]", Array<Int>(3) { it + 1 }, default)
        assertSerializedWithType("""["1","2","3"]""", Array<String>(3) { (it + 1).toString() }, default)
        assertSerializedWithType("[[0],[1],[2]]", Array<Array<Int>>(3) { cnt -> Array(1) { cnt } }, default)
        noLegacyJs {
            assertSerializedWithType("""[{"boxed":"foo"}]""", Array(1) { Box("foo") }, default)
            assertSerializedWithType("""[[{"boxed":"foo"}]]""", Array(1) { Array(1) { Box("foo") } }, default)
        }
    }

    @Test
    fun testPrimitiveArrays() {
        assertSerializedWithType("[1,2,3]", intArrayOf(1, 2, 3), default)
        assertSerializedWithType("[1,2,3]", longArrayOf(1, 2, 3), default)
        assertSerializedWithType("[1,2,3]", byteArrayOf(1, 2, 3), default)
        assertSerializedWithType("[1,2,3]", shortArrayOf(1, 2, 3), default)
        assertSerializedWithType("[true,false]", booleanArrayOf(true, false), default)
        assertSerializedWithType("""["a","b","c"]""", charArrayOf('a', 'b', 'c'), default)
    }

    @Test
    fun testSerializableObject() = noLegacyJs {
        assertSerializedWithType("{}", SampleObject)
    }

    class IntBox(val i: Int)

    class CustomIntSerializer(isNullable: Boolean) : KSerializer<IntBox?> {
        override val descriptor: SerialDescriptor

        init {
            val d = PrimitiveSerialDescriptor("CIS", PrimitiveKind.INT)
            descriptor = if (isNullable) d.nullable else d
        }

        override fun serialize(encoder: Encoder, value: IntBox?) {
            if (value == null) encoder.encodeInt(41)
            else encoder.encodeInt(42)
        }

        override fun deserialize(decoder: Decoder): IntBox? {
            TODO()
        }
    }

    @Test
    fun testContextualLookup() {
        val module = SerializersModule { contextual(CustomIntSerializer(false).cast<IntBox>()) }
        val json = Json { serializersModule = module }
        val data = listOf(listOf(IntBox(1)))
        assertEquals("[[42]]", json.encodeToString(data))
    }

    @Test
    fun testContextualLookupNullable() {
        val module = SerializersModule { contextual(CustomIntSerializer(true).cast<IntBox>()) }
        val serializer = module.serializer<List<List<IntBox?>>>()
        assertEquals("[[41]]", Json.encodeToString(serializer, listOf(listOf<IntBox?>(null))))
    }

    @Test
    fun testContextualLookupNonNullable() {
        val module = SerializersModule { contextual(CustomIntSerializer(false).cast<IntBox>()) }
        val serializer = module.serializer<List<List<IntBox?>>>()
        assertEquals("[[null]]", Json.encodeToString(serializer, listOf(listOf<IntBox?>(null))))
    }

    @Test
    fun testCompiledWinsOverContextual() {
        val contextual = object : KSerializer<Int> {
            override val descriptor: SerialDescriptor = Int.serializer().descriptor

            override fun serialize(encoder: Encoder, value: Int) {
                fail()
            }

            override fun deserialize(decoder: Decoder): Int {
                fail()
            }
        }
        val json = Json { serializersModule = SerializersModule { contextual(contextual) } }
        assertEquals("[[1]]", json.encodeToString(listOf(listOf<Int>(1))))
        assertEquals("42", json.encodeToString(42))
    }

    class NonSerializable

    class NonSerializableBox<T>(val boxed: T)

    @Test
    fun testLookupFail() {
        assertNull(serializerOrNull(typeOf<NonSerializable>()))
        assertNull(serializerOrNull(typeOf<NonSerializableBox<String>>()))
        assertNull(serializerOrNull(typeOf<Box<NonSerializable>>()))

        assertFailsWithMessage<SerializationException>("for class 'NonSerializable'") {
            serializer(typeOf<NonSerializable>())
        }

        assertFailsWithMessage<SerializationException>("for class 'NonSerializableBox'") {
            serializer(typeOf<NonSerializableBox<String>>())
        }

        assertFailsWithMessage<SerializationException>("for class 'NonSerializable'") {
            serializer(typeOf<Box<NonSerializable>>())
        }
    }

    private inline fun <reified T> assertSerializedWithType(
        expected: String,
        value: T,
        json: StringFormat = default
    ) {
        val serial = serializer<T>()
        assertEquals(expected, json.encodeToString(serial, value))
        val serial2 = requireNotNull(serializerOrNull(typeOf<T>())) { "Expected serializer to be found" }
        assertEquals(expected, json.encodeToString(serial2, value))
    }

    inline fun <T> KSerializer<*>.cast(): KSerializer<T> = this as KSerializer<T>

}
