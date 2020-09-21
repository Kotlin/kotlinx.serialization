/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import org.junit.Test
import java.lang.reflect.*
import kotlin.test.*

class SerializerByTypeTest {

    val json = Json { }

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
        val serial = serializer(IntBoxToken)
        val s = json.encodeToString(serial, b)
        assertEquals("""{"a":42}""", s)
    }

    @Test
    fun testArray() {
        val myArr = arrayOf("a", "b", "c")
        val token = myArr::class.java
        val serial = serializer(token)
        val s = json.encodeToString(serial, myArr)
        assertEquals("""["a","b","c"]""", s)
    }

    @Test
    fun testList() {
        val myArr = listOf("a", "b", "c")
        val token = object : ParameterizedType {
            override fun getRawType(): Type = List::class.java
            override fun getOwnerType(): Type? = null
            override fun getActualTypeArguments(): Array<Type> = arrayOf(String::class.java)
        }
        val serial = serializer(token)
        val s = json.encodeToString(serial, myArr)
        assertEquals("""["a","b","c"]""", s)
    }


    @Test
    fun testReifiedArrayResolving() {
        val myArr = arrayOf("a", "b", "c")
        val token = typeTokenOf<Array<String>>()
        val serial = serializer(token)
        val s = json.encodeToString(serial, myArr)
        assertEquals("""["a","b","c"]""", s)
    }

    @Test
    fun testReifiedListResolving() {
        val myList = listOf("a", "b", "c")
        val token = typeTokenOf<List<String>>()
        val serial = serializer(token)
        val s = json.encodeToString(serial, myList)
        assertEquals("""["a","b","c"]""", s)
    }

    @Test
    fun testReifiedSetResolving() {
        val mySet = setOf("a", "b", "c", "c")
        val token = typeTokenOf<Set<String>>()
        val serial = serializer(token)
        val s = json.encodeToString(serial, mySet)
        assertEquals("""["a","b","c"]""", s)
    }

    @Test
    fun testReifiedMapResolving() {
        val myMap = mapOf("a" to Data(listOf("c"), Box(6)))
        val token = typeTokenOf<Map<String, Data>>()
        val serial = serializer(token)
        val s = json.encodeToString(serial, myMap)
        assertEquals("""{"a":{"l":["c"],"b":{"a":6}}}""", s)
    }

    @Test
    fun testNestedListResolving() {
        val myList = listOf(listOf(listOf(1, 2, 3)), listOf())
        val token = typeTokenOf<List<List<List<Int>>>>()
        val serial = serializer(token)
        val s = json.encodeToString(serial, myList)
        assertEquals("[[[1,2,3]],[]]", s)
    }

    @Test
    fun testNestedArrayResolving() {
        val myList = arrayOf(arrayOf(arrayOf(1, 2, 3)), arrayOf())
        val token = typeTokenOf<Array<Array<Array<Int>>>>()
        val serial = serializer(token)
        val s = json.encodeToString(serial, myList)
        assertEquals("[[[1,2,3]],[]]", s)
    }

    @Test
    fun testNestedMixedResolving() {
        val myList = arrayOf(listOf(arrayOf(1, 2, 3)), listOf())
        val token = typeTokenOf<Array<List<Array<Int>>>>()
        val serial = serializer(token)
        val s = json.encodeToString(serial, myList)
        assertEquals("[[[1,2,3]],[]]", s)
    }

    @Test
    fun testGenericInHolder() {
        val b = Data(listOf("a", "b", "c"), Box(42))
        val serial = serializer(Data::class.java)
        val s = json.encodeToString(serial, b)
        assertEquals("""{"l":["a","b","c"],"b":{"a":42}}""", s)
    }

    @Test
    fun testOverriddenSerializer() {
        val foo = json.decodeFromString<WithCustomDefault>("9")
        assertEquals(9, foo.n)
    }

    @Test
    fun testNamedCompanion() {
        val namedCompanion = WithNamedCompanion(1)
        val serial = serializer(WithNamedCompanion::class.java)
        val s = json.encodeToString(serial, namedCompanion)
        assertEquals("""{"a":1}""", s)
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
}
