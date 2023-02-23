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

    private fun testMemorySize(simple: Simple, str: String) {
        val res = Hocon.encodeToConfig(simple)
        res.assertContains(str)
        assertEquals(simple, Hocon.decodeFromConfig(Simple.serializer(), res))
    }

    @Test
    fun testSerializeMemorySize() {
        testMemorySize(Simple(ofBytes(10)), "size = 10 byte")
        testMemorySize(Simple(ofBytes(1000)), "size = 1000 byte")

        val oneKib = BigInteger.valueOf(1024)
        testMemorySize(Simple(ofBytes(oneKib)), "size = 1 KiB")
        testMemorySize(Simple(ofBytes(oneKib + BigInteger.ONE)), "size = 1025 byte")

        val oneMib = oneKib * oneKib
        testMemorySize(Simple(ofBytes(oneMib)), "size = 1 MiB")
        testMemorySize(Simple(ofBytes(oneMib + BigInteger.ONE)), "size = ${oneMib + BigInteger.ONE} byte")
        testMemorySize(Simple(ofBytes(oneMib + oneKib)), "size = 1025 KiB")

        val oneGib = oneMib * oneKib
        testMemorySize(Simple(ofBytes(oneGib)), "size = 1 GiB")
        testMemorySize(Simple(ofBytes(oneGib + BigInteger.ONE)), "size = ${oneGib + BigInteger.ONE} byte")
        testMemorySize(Simple(ofBytes(oneGib + oneKib)), "size = ${oneMib + BigInteger.ONE} KiB")
        testMemorySize(Simple(ofBytes(oneGib + oneMib)), "size = 1025 MiB")

        val oneTib = oneGib * (oneKib)
        testMemorySize(Simple(ofBytes(oneTib)), "size = 1 TiB")
        testMemorySize(Simple(ofBytes(oneTib + BigInteger.ONE)), "size = ${oneTib.add(BigInteger.ONE)} byte")
        testMemorySize(Simple(ofBytes(oneTib + oneKib)), "size = ${oneGib + BigInteger.ONE} KiB")
        testMemorySize(Simple(ofBytes(oneTib + oneMib)), "size = ${oneMib + BigInteger.ONE} MiB")
        testMemorySize(Simple(ofBytes(oneTib + oneGib)), "size = 1025 GiB")

        val onePib = oneTib * oneKib
        testMemorySize(Simple(ofBytes(onePib)), "size = 1 PiB")
        testMemorySize(Simple(ofBytes(onePib + BigInteger.ONE)), "size = ${onePib + BigInteger.ONE} byte")

        val oneEib = onePib * oneKib
        testMemorySize(Simple(ofBytes(oneEib)), "size = 1 EiB")
        testMemorySize(Simple(ofBytes(oneEib + BigInteger.ONE)), "size = ${oneEib + BigInteger.ONE} byte")

        val oneZib = oneEib * oneKib
        testMemorySize(Simple(ofBytes(oneZib)), "size = 1 ZiB")
        testMemorySize(Simple(ofBytes(oneZib + BigInteger.ONE)), "size = ${oneZib + BigInteger.ONE} byte")

        val oneYib = oneZib * oneKib
        testMemorySize(Simple(ofBytes(oneYib)), "size = 1 YiB")
        testMemorySize(Simple(ofBytes(oneYib + BigInteger.ONE)), "size = ${oneYib + BigInteger.ONE} byte")
        testMemorySize(Simple(ofBytes(oneYib * oneKib)), "size = $oneKib YiB")
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
