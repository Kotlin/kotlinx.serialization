/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.hocon

import kotlinx.serialization.*
import org.junit.*
import org.junit.Assert.*

class HoconValuesTest {

    @Serializable
    data class NumbersConfig(val b: Byte, val s: Short, val i: Int, val l: Long, val f: Float, val d: Double)

    enum class Choice { A, B, C }

    @Serializable
    data class StringConfig(val c: Char, val s: String)

    @Serializable
    data class OtherConfig(val b: Boolean, val e: Choice, val u: Unit = Unit)

    @Serializable
    data class WithDefault(val i: Int = 5, val s: String = "foo")

    @Serializable
    data class WithNullable(val i: Int?, val s: String?)

    @Serializable
    data class WithNullableList(val i1: List<Int?>, val i2: List<String>?, val i3: List<WithNullable?>?)

    @Test
    fun `deserialize numbers`() {
        val conf = "b=42, s=1337, i=100500, l = 4294967294, f=0.0, d=-0.123"
        val nums = deserializeConfig(conf, NumbersConfig.serializer())
        with(nums) {
            assertEquals(42.toByte(), b)
            assertEquals(1337.toShort(), s)
            assertEquals(100500, i)
            assertEquals(4294967294L, l)
            assertEquals(0.0f, f)
            assertEquals(-0.123, d, 1e-9)
        }
    }

    @Test
    fun `deserialize string types`() {
        val obj = deserializeConfig("c=f, s=foo", StringConfig.serializer())
        assertEquals('f', obj.c)
        assertEquals("foo", obj.s)
    }

    @Test
    fun `deserialize other types`() {
        val obj = deserializeConfig("e = A, b=true", OtherConfig.serializer())
        assertEquals(Choice.A, obj.e)
        assertEquals(true, obj.b)
    }

    @Test
    fun `deserialize default values`() {
        val obj = deserializeConfig("", WithDefault.serializer())
        assertEquals(5, obj.i)
        assertEquals("foo", obj.s)
    }

    @Test
    fun `overwrite default values`() {
        val obj = deserializeConfig("i = 42, s = bar", WithDefault.serializer())
        assertEquals(42, obj.i)
        assertEquals("bar", obj.s)
    }

    @Test
    fun `deserialize nullable types`() {
        val obj = deserializeConfig("i = 10, s = null", WithNullable.serializer())
        assertEquals(10, obj.i)
        assertEquals(null, obj.s)
    }

    @Test
    fun `deserialize complex nullable values`() {
        val configString = "i1 = [1,null,3], i2=null, i3 = [null, {i: 10, s: bar}]"
        val obj = deserializeConfig(configString, WithNullableList.serializer())
        with(obj) {
            assertEquals(listOf(1, null, 3), i1)
            assertEquals(null, i2)
            assertEquals(listOf(null, WithNullable(10, "bar")), i3)
        }
    }
}
