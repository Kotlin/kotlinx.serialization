/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor

import com.upokecenter.cbor.*
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import org.junit.Test
import kotlin.test.*

class CborCompatibilityTest {

    @Serializable
    data class SomeClass(val prop: Int = 0)

    @Serializable
    data class WithMap(val map: Map<Long, Long>)
    @Serializable
    data class IntData(val intV: Int)
    @Serializable
    data class StringData(val data: String)
    @Serializable
    data class FloatData(val field: Float)
    @Serializable
    data class DoubleData(val field: Double)

    @Serializable
    data class SomeComplexClass<T>(
        val boxed: T,
        val otherClass: StringData,
        val primitive: Int,
        val map: Map<String, IntData>
    )

    private inline fun <reified T> compare(obj: T, serializer: KSerializer<T>) {
        val bytes = CBORObject.FromObject(obj).EncodeToBytes()
        assertEquals(obj, Cbor.decodeFromByteArray(serializer, bytes))
    }

    private fun compareDouble(value: Double) {
        val doubleWrapper = DoubleData(value)
        val bytes = CBORObject.FromObject(doubleWrapper).EncodeToBytes()
        assertEquals(doubleWrapper, Cbor.decodeFromByteArray(DoubleData.serializer(), bytes))
    }

    private fun compareFloat(value: Float) {
        val floatWrapper = FloatData(value)
        val bytes = CBORObject.FromObject(floatWrapper).EncodeToBytes()
        assertEquals(floatWrapper, Cbor.decodeFromByteArray(FloatData.serializer(), bytes))
    }

    @Test
    fun basicClassFromAnotherLibrary() {
        compare(SomeClass(), SomeClass.serializer())
    }

    @Test
    fun basicListFromAnotherLibrary() {
        compare(
            listOf(
                SomeClass(1),
                SomeClass(2),
                SomeClass(3)
            ), ListSerializer(SomeClass.serializer())
        )
    }

    @Test
    fun withMap() {
        compare(WithMap(mapOf()), WithMap.serializer())
        compare(WithMap(mapOf(10L to 10L)), WithMap.serializer())
        compare(
            WithMap(
                mapOf(
                    10L to 10L,
                    20L to 20L
                )
            ), WithMap.serializer())
    }

    @Test
    fun someComplexClass() {
        val obj = SomeComplexClass(
            listOf(10),
            StringData("20"),
            30,
            mapOf("40" to IntData(40), "50" to IntData(50))
        )
        val serial = SomeComplexClass.serializer(ListSerializer(Int.serializer()))
        compare(obj, serial)
    }

    @Test
    fun testFloat() {
        compareFloat(Float.NaN)
        compareFloat(Float.POSITIVE_INFINITY)
        compareFloat(Float.NEGATIVE_INFINITY)
        compareFloat(Float.MAX_VALUE)
        compareFloat(Float.MIN_VALUE)
        compareFloat(0.0f)
        compareFloat(-0.0f)
        compareFloat(-1.0f)
        compareFloat(1.0f)
        compareFloat(123.56f)
        compareFloat(123.0f)
        // minimal denormalized value in half-precision
        compareFloat(5.9604645E-8f)
        // maximal denormalized value in half-precision
        compareFloat(0.000060975552f)
    }

    @Test
    fun testDouble() {
        compareDouble(Double.NaN)
        compareDouble(Double.POSITIVE_INFINITY)
        compareDouble(Double.NEGATIVE_INFINITY)
        compareDouble(Double.MAX_VALUE)
        compareDouble(Double.MIN_VALUE)
        compareDouble(0.0)
        compareDouble(-0.0)
        compareDouble(-1.0)
        compareDouble(1.0)
        compareDouble(123.56)
        compareDouble(123.0)
        // minimal denormalized value in half-precision
        compareDouble(5.9604644775390625E-8)
        // maximal denormalized value in half-precision
        compareDouble(0.00006097555160522461)
    }
}
