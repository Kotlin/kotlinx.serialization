@file:UseSerializers(ConfigMemorySizeSerializer::class)
package kotlinx.serialization.hocon

import com.typesafe.config.*
import com.typesafe.config.ConfigMemorySize.ofBytes
import java.math.BigInteger
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.hocon.serializers.ConfigMemorySizeSerializer
import kotlinx.serialization.modules.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.test.assertFailsWith

class HoconMemorySizeTest {

    @Serializable
    data class Simple(val size: ConfigMemorySize)

    @Serializable
    data class Nullable(val size: ConfigMemorySize?)

    @Serializable
    data class ConfigList(val l: List<ConfigMemorySize>)

    @Serializable
    data class ConfigMap(val mp: Map<String, ConfigMemorySize>)

    @Serializable
    data class ConfigMapMemoryKey(val mp: Map<ConfigMemorySize, ConfigMemorySize>)

    @Serializable
    data class Complex(
        val i: Int,
        val s: Simple,
        val n: Nullable,
        val l: List<Simple>,
        val ln: List<Nullable>,
        val f: Boolean,
        val ld: List<ConfigMemorySize>,
        val mp: Map<String, ConfigMemorySize>,
        val mpp: Map<ConfigMemorySize, ConfigMemorySize>
    )

    @Test
    fun testSerializeMemorySize() {
        Hocon.encodeToConfig(Simple(ofBytes(10))).assertContains("size = 10 byte")
        Hocon.encodeToConfig(Simple(ofBytes(1000))).assertContains("size = 1000 byte")
        val oneKib = BigInteger.valueOf(1024)
        Hocon.encodeToConfig(Simple(ofBytes(oneKib))).assertContains("size = 1 KiB")
        Hocon.encodeToConfig(Simple(ofBytes(oneKib + BigInteger.ONE))).assertContains("size = 1025 byte")
        val oneMib = oneKib * oneKib
        Hocon.encodeToConfig(Simple(ofBytes(oneMib))).assertContains("size = 1 MiB")
        Hocon.encodeToConfig(Simple(ofBytes(oneMib + BigInteger.ONE))).assertContains("size = ${oneMib + BigInteger.ONE} byte")
        Hocon.encodeToConfig(Simple(ofBytes(oneMib + oneKib))).assertContains("size = 1025 KiB")
        val oneGib = oneMib * oneKib
        Hocon.encodeToConfig(Simple(ofBytes(oneGib))).assertContains("size = 1 GiB")
        Hocon.encodeToConfig(Simple(ofBytes(oneGib + BigInteger.ONE))).assertContains("size = ${oneGib + BigInteger.ONE} byte")
        Hocon.encodeToConfig(Simple(ofBytes(oneGib + oneKib))).assertContains("size = ${oneMib + BigInteger.ONE} KiB")
        Hocon.encodeToConfig(Simple(ofBytes(oneGib + oneMib))).assertContains("size = 1025 MiB")
        val oneTib = oneGib * (oneKib)
        Hocon.encodeToConfig(Simple(ofBytes(oneTib))).assertContains("size = 1 TiB")
        Hocon.encodeToConfig(Simple(ofBytes(oneTib + BigInteger.ONE))).assertContains("size = ${oneTib.add(BigInteger.ONE)} byte")
        Hocon.encodeToConfig(Simple(ofBytes(oneTib + oneKib))).assertContains("size = ${oneGib + BigInteger.ONE} KiB")
        Hocon.encodeToConfig(Simple(ofBytes(oneTib + oneMib))).assertContains("size = ${oneMib + BigInteger.ONE} MiB")
        Hocon.encodeToConfig(Simple(ofBytes(oneTib + oneGib))).assertContains("size = 1025 GiB")
        val onePib = oneTib * oneKib
        Hocon.encodeToConfig(Simple(ofBytes(onePib))).assertContains("size = 1 PiB")
        Hocon.encodeToConfig(Simple(ofBytes(onePib + BigInteger.ONE))).assertContains("size = ${onePib + BigInteger.ONE} byte")
        val oneEib = onePib * oneKib
        Hocon.encodeToConfig(Simple(ofBytes(oneEib))).assertContains("size = 1 EiB")
        Hocon.encodeToConfig(Simple(ofBytes(oneEib + BigInteger.ONE))).assertContains("size = ${oneEib + BigInteger.ONE} byte")
        val oneZib = oneEib * oneKib
        Hocon.encodeToConfig(Simple(ofBytes(oneZib))).assertContains("size = 1 ZiB")
        Hocon.encodeToConfig(Simple(ofBytes(oneZib + BigInteger.ONE))).assertContains("size = ${oneZib + BigInteger.ONE} byte")
        val oneYib = oneZib * oneKib
        Hocon.encodeToConfig(Simple(ofBytes(oneYib))).assertContains("size = 1 YiB")
        Hocon.encodeToConfig(Simple(ofBytes(oneYib + BigInteger.ONE))).assertContains("size = ${oneYib + BigInteger.ONE} byte")
        Hocon.encodeToConfig(Simple(ofBytes(oneYib * oneKib))).assertContains("size = $oneKib YiB")
    }

    @Test
    fun testSerializeNullableMemorySize() {
        Hocon.encodeToConfig(Nullable(null)).assertContains("size = null")
        Hocon.encodeToConfig(Nullable(ofBytes(1024 * 6))).assertContains("size = 6 KiB")
    }

    @Test
    fun testSerializeListOfMemorySize() {
        Hocon.encodeToConfig(ConfigList(listOf(ofBytes(1), ofBytes(1024 * 1024), ofBytes(1024))))
            .assertContains("l: [ 1 byte, 1 MiB, 1 KiB ]")
    }

    @Test
    fun testSerializeMapOfMemorySize() {
        Hocon.encodeToConfig(ConfigMap(mapOf("one" to ofBytes(2000), "two" to ofBytes(1024 * 1024 * 1024))))
            .assertContains("mp: { one = 2000 byte, two = 1 GiB }")
        Hocon.encodeToConfig(ConfigMapMemoryKey((mapOf(ofBytes(1024) to ofBytes(1024)))))
            .assertContains("mp: { 1 KiB = 1 KiB }")
    }

    @Test
    fun testDeserializeMemorySize() {
        var obj = deserializeConfig("size = 1 Ki", Simple.serializer())
        assertEquals(ofBytes(1024), obj.size)
        obj = deserializeConfig("size = 1 MB", Simple.serializer())
        assertEquals(ofBytes(1_000_000), obj.size)
        obj = deserializeConfig("size = 1 byte", Simple.serializer())
        assertEquals(ofBytes(1), obj.size)
    }

    @Test
    fun testDeserializeNullableMemorySize() {
        var obj = deserializeConfig("size = null", Nullable.serializer())
        assertNull(obj.size)
        obj = deserializeConfig("size = 5 byte", Nullable.serializer())
        assertEquals(ofBytes(5), obj.size)
    }

    @Test
    fun testDeserializeListOfMemorySize() {
        val obj = deserializeConfig("l: [ 1b, 1MB, 1Ki ]", ConfigList.serializer())
        assertEquals(listOf(ofBytes(1), ofBytes(1_000_000), ofBytes(1024)), obj.l)
    }

    @Test
    fun testDeserializeMapOfMemorySize() {
        val obj = deserializeConfig("""
             mp: { one = 2kB, two = 5 MB }
        """.trimIndent(), ConfigMap.serializer())
        assertEquals(mapOf("one" to ofBytes(2000), "two" to ofBytes(5_000_000)), obj.mp)

        val objDurationKey = deserializeConfig("""
             mp: { 1024b = 1Ki }
        """.trimIndent(), ConfigMapMemoryKey.serializer())
        assertEquals(mapOf(ofBytes(1024) to ofBytes(1024)), objDurationKey.mp)
    }

    @Test
    fun testDeserializeComplexMemorySize() {
        val obj = deserializeConfig("""
            i = 6
            s: { size = 5 MB }
            n: { size = null }
            l: [ { size = 1 kB }, { size = 2b } ]
            ln: [ { size = null }, { size = 1 Mi } ]
            f = true
            ld: [ 1 kB, 1 m]
            mp: { one = 2kB, two = 5 MB }
            mpp: { 1024b = 1Ki }
        """.trimIndent(), Complex.serializer())
        assertEquals(ofBytes(5_000_000), obj.s.size)
        assertNull(obj.n.size)
        assertEquals(listOf(Simple(ofBytes(1000)), Simple(ofBytes(2))), obj.l)
        assertEquals(listOf(Nullable(null), Nullable(ofBytes(1024 * 1024))), obj.ln)
        assertEquals(6, obj.i)
        assertTrue(obj.f)
        assertEquals(listOf(ofBytes(1000), ofBytes(1048576)), obj.ld)
        assertEquals(mapOf("one" to ofBytes(2000), "two" to ofBytes(5_000_000)), obj.mp)
        assertEquals(mapOf(ofBytes(1024) to ofBytes(1024)), obj.mpp)
    }

    @Test
    fun testThrowsWhenNotSizeFormatHocon() {
        val message = "Value at size cannot be read as ConfigMemorySize because it is not a valid HOCON Size value"
        assertFailsWith<SerializationException>(message) {
            deserializeConfig("size = 1 unknown", Simple.serializer())
        }
    }
}
