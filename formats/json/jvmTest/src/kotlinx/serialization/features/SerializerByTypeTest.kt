/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.test.*
import org.junit.Test
import java.lang.reflect.*
import kotlin.reflect.*
import kotlin.test.*

class SerializerByTypeTest {

    private val json = Json

    @Serializable
    data class Box<out T>(val a: T)

    @Serializable
    data class Data(val l: List<String>, val b: Box<Int>)

    @Serializable
    data class WithCustomDefault(val n: Int) {
        @Serializer(forClass = WithCustomDefault::class)
        companion object {
            override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("WithCustomDefault", PrimitiveKind.INT)
            override fun serialize(encoder: Encoder, value: WithCustomDefault) = encoder.encodeInt(value.n)
            override fun deserialize(decoder: Decoder) = WithCustomDefault(decoder.decodeInt())
        }
    }

    object IntBoxToken : ParameterizedType {
        override fun getRawType() = Box::class.java
        override fun getOwnerType() = null
        override fun getActualTypeArguments(): Array<Type> = arrayOf(Int::class.java)
    }

    @Serializable
    object SerializableObject

    @Serializable
    data class WithNamedCompanion(val a: Int) {
        companion object Named
    }

    @Test
    fun testGenericParameter() {
        val b = Box(42)
        assertSerializedWithType(IntBoxToken, """{"a":42}""", b)
    }

    @Test
    fun testNestedGenericParameter() {
        val b = Box(Box(239))
        assertSerializedWithType(typeTokenOf<Box<Box<Int>>>(), """{"a":{"a":239}}""", b)
    }

    @Test
    fun testArray() {
        val myArr = arrayOf("a", "b", "c")
        val token = myArr::class.java
        assertSerializedWithType(token, """["a","b","c"]""", myArr)
    }

    @Test
    fun testList() {
        val myArr = listOf("a", "b", "c")
        val token = object : ParameterizedType {
            override fun getRawType(): Type = List::class.java
            override fun getOwnerType(): Type? = null
            override fun getActualTypeArguments(): Array<Type> = arrayOf(String::class.java)
        }
        assertSerializedWithType(token, """["a","b","c"]""", myArr)
    }

    @Test
    fun testListAsCollection() {
        val myArr: Collection<String> = listOf("a", "b", "c")
        val token = object : ParameterizedType {
            override fun getRawType(): Type = Collection::class.java
            override fun getOwnerType(): Type? = null
            override fun getActualTypeArguments(): Array<Type> = arrayOf(String::class.java)
        }
        assertSerializedWithType(token, """["a","b","c"]""", myArr)
    }


    @Test
    fun testReifiedArrayResolving() {
        val myArr = arrayOf("a", "b", "c")
        val token = typeTokenOf<Array<String>>()
        assertSerializedWithType(token, """["a","b","c"]""", myArr)
    }

    @Test
    fun testPrimitiveArrayResolving() {
        val myArr = intArrayOf(1, 2, 3)
        val token = IntArray::class.java
        val name = serializer(token).descriptor.serialName
        assertTrue(name.contains("IntArray"))
        assertSerializedWithType(token, """[1,2,3]""", myArr)
    }

    @Test
    fun testReifiedListResolving() {
        val myList = listOf("a", "b", "c")
        val token = typeTokenOf<List<String>>()
        assertSerializedWithType(token, """["a","b","c"]""", myList)
    }

    @Test
    fun testReifiedSetResolving() {
        val mySet = setOf("a", "b", "c", "c")
        val token = typeTokenOf<Set<String>>()
        assertSerializedWithType(token, """["a","b","c"]""", mySet)
    }

    @Test
    fun testReifiedMapResolving() {
        val myMap = mapOf("a" to Data(listOf("c"), Box(6)))
        val token = typeTokenOf<Map<String, Data>>()
        assertSerializedWithType(token, """{"a":{"l":["c"],"b":{"a":6}}}""",myMap)
    }

    @Test
    fun testNestedListResolving() {
        val myList = listOf(listOf(listOf(1, 2, 3)), listOf())
        val token = typeTokenOf<List<List<List<Int>>>>()
        assertSerializedWithType(token, "[[[1,2,3]],[]]", myList)
    }

    @Test
    fun testNestedArrayResolving() {
        val myList = arrayOf(arrayOf(arrayOf(1, 2, 3)), arrayOf())
        val token = typeTokenOf<Array<Array<Array<Int>>>>()
        assertSerializedWithType(token, "[[[1,2,3]],[]]", myList)
    }

    @Test
    fun testNestedMixedResolving() {
        val myList = arrayOf(listOf(arrayOf(1, 2, 3)), listOf())
        val token = typeTokenOf<Array<List<Array<Int>>>>()
        assertSerializedWithType(token, "[[[1,2,3]],[]]", myList)
    }

    @Test
    fun testPair() {
        val myPair = "42" to 42
        val token = typeTokenOf<Pair<String, Int>>()
        assertSerializedWithType(token, """{"first":"42","second":42}""", myPair)
    }

    @Test
    fun testTriple() {
        val myTriple = Triple("1", 2, Box(42))
        val token = typeTokenOf<Triple<String, Int, Box<Int>>>()
        assertSerializedWithType(token, """{"first":"1","second":2,"third":{"a":42}}""", myTriple)
    }

    @Test
    fun testGenericInHolder() {
        val b = Data(listOf("a", "b", "c"), Box(42))
        assertSerializedWithType(Data::class.java,"""{"l":["a","b","c"],"b":{"a":42}}""", b )
    }

    @Test
    fun testOverriddenSerializer() {
        val foo = json.decodeFromString<WithCustomDefault>("9")
        assertEquals(9, foo.n)
    }

    @Test
    fun testNamedCompanion() {
        val namedCompanion = WithNamedCompanion(1)
        assertSerializedWithType(WithNamedCompanion::class.java, """{"a":1}""", namedCompanion)
    }

    @Test
    fun testPrimitive() {
        val token = typeTokenOf<Int>()
        val serial = serializer(token)
        assertSame(Int.serializer() as KSerializer<*>, serial)
    }

    @Test
    fun testObject() {
        val token = typeTokenOf<SerializableObject>()
        val serial = serializer(token)
        assertEquals(SerializableObject.serializer().descriptor, serial.descriptor)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> assertSerializedWithType(
        token: Type,
        expected: String,
        value: T,
    ) {
        val serial = serializer(token) as KSerializer<T>
        assertEquals(expected, json.encodeToString(serial, value))
        val serial2 = requireNotNull(serializerOrNull(token)) { "Expected serializer to be found" }
        assertEquals(expected, json.encodeToString(serial2 as KSerializer<T>, value))
    }

    @PublishedApi
    internal open class TypeBase<T>

    public inline fun <reified T> typeTokenOf(): Type {
        val base = object : TypeBase<T>() {}
        val superType = base::class.java.genericSuperclass!!
        return (superType as ParameterizedType).actualTypeArguments.first()!!
    }

    class IntBox(val i: Int)

    object CustomIntSerializer : KSerializer<IntBox> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("CIS", PrimitiveKind.INT)

        override fun serialize(encoder: Encoder, value: IntBox) {
            encoder.encodeInt(42)
        }

        override fun deserialize(decoder: Decoder): IntBox {
            TODO()
        }
    }

    @Test
    fun testContextualLookup() {
        val module = SerializersModule { contextual(CustomIntSerializer) }
        val serializer = module.serializer(typeTokenOf<List<List<IntBox>>>())
        assertEquals("[[42]]", Json.encodeToString(serializer, listOf(listOf(IntBox(1)))))
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
        val module = SerializersModule { contextual(contextual) }
        val serializer = module.serializer(typeTokenOf<List<List<Int>>>())
        assertEquals("[[1]]", Json.encodeToString(serializer, listOf(listOf<Int>(1))))
        assertEquals("42", Json.encodeToString(module.serializer(typeTokenOf<Int>()), 42))
    }

    class NonSerializable

    class NonSerializableBox<T>(val boxed: T)

    @Test
    fun testLookupFail() {
        assertNull(serializerOrNull(typeTokenOf<NonSerializable>()))
        assertNull(serializerOrNull(typeTokenOf<NonSerializableBox<String>>()))
        assertNull(serializerOrNull(typeTokenOf<Box<NonSerializable>>()))

        assertFailsWithMessage<SerializationException>("for class 'NonSerializable'") {
            serializer(typeTokenOf<NonSerializable>())
        }

        assertFailsWithMessage<SerializationException>("for class 'NonSerializableBox'") {
            serializer(typeTokenOf<NonSerializableBox<String>>())
        }

        assertFailsWithMessage<SerializationException>("for class 'NonSerializable'") {
            serializer(typeTokenOf<kotlinx.serialization.Box<NonSerializable>>())
        }

        assertFailsWithMessage<SerializationException>("for class 'NonSerializable'") {
            serializer(typeTokenOf<Array<NonSerializable>>())
        }
    }
}
