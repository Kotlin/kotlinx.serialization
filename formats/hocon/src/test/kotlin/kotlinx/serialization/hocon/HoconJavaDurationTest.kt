@file:UseSerializers(JavaDurationSerializer::class)
package kotlinx.serialization.hocon

import java.time.Duration
import java.time.Duration.*
import kotlin.test.assertFailsWith
import kotlinx.serialization.*
import kotlinx.serialization.hocon.serializers.JavaDurationSerializer
import org.junit.*
import org.junit.Assert.*

class HoconJavaDurationTest {

    @Serializable
    data class Simple(val d: Duration)

    @Serializable
    data class Nullable(val d: Duration?)

    @Serializable
    data class ConfigList(val ld: List<Duration>)

    @Serializable
    data class ConfigMap(val mp: Map<String, Duration>)

    @Serializable
    data class ConfigMapDurationKey(val mp: Map<Duration, Duration>)

    @Serializable
    data class Complex(
        val i: Int,
        val s: Simple,
        val n: Nullable,
        val l: List<Simple>,
        val ln: List<Nullable>,
        val f: Boolean,
        val ld: List<Duration>,
        val mp: Map<String, Duration>,
        val mpp: Map<Duration, Duration>
    )

    private fun testJavaDuration(simple: Simple, str: String) {
        val res = Hocon.encodeToConfig(simple)
        res.assertContains(str)
        assertEquals(simple, Hocon.decodeFromConfig(Simple.serializer(), res))
    }

    @Test
    fun testSerializeDuration() {
        testJavaDuration(Simple(ofMinutes(10)), "d = 10 m")
        testJavaDuration(Simple(ofSeconds(120)), "d = 2 m")
        testJavaDuration(Simple(ofHours(1)), "d = 1 h")
        testJavaDuration(Simple(ofMinutes(120)), "d = 2 h")
        testJavaDuration(Simple(ofSeconds(3600 * 3)), "d = 3 h")
        testJavaDuration(Simple(ofDays(3)), "d = 3 d")
        testJavaDuration(Simple(ofHours(24)), "d = 1 d")
        testJavaDuration(Simple(ofMinutes(1440 * 2)), "d = 2 d")
        testJavaDuration(Simple(ofSeconds(86400 * 4)), "d = 4 d")
        testJavaDuration(Simple(ofSeconds(1)), "d = 1 s")
        testJavaDuration(Simple(ofMinutes(2).plusSeconds(1)), "d = 121 s")
        testJavaDuration(Simple(ofHours(1).plusSeconds(1)), "d = 3601 s")
        testJavaDuration(Simple(ofDays(1).plusSeconds(5)), "d = 86405 s")
        testJavaDuration(Simple(ofNanos(9)), "d = 9 ns")
        testJavaDuration(Simple(ofNanos(1_000_000).plusSeconds(5)), "d = 5001 ms")
        testJavaDuration(Simple(ofNanos(1_000).plusSeconds(9)), "d = 9000001 us")
        testJavaDuration(Simple(ofNanos(1_000_005).plusSeconds(5)), "d = 5001000005 ns")
        testJavaDuration(Simple(ofNanos(1_002).plusSeconds(9)), "d = 9000001002 ns")
        testJavaDuration(Simple(ofNanos(1_000_000_001)), "d = 1000000001 ns")
        testJavaDuration(Simple(ofDays(-10)), "d = -10 d")
    }

    @Test
    fun testSerializeNullableDuration() {
        Hocon.encodeToConfig(Nullable(null)).assertContains("d = null")
        Hocon.encodeToConfig(Nullable(ofSeconds(6))).assertContains("d = 6 s")
    }

    @Test
    fun testSerializeListOfDuration() {
        Hocon.encodeToConfig(ConfigList(listOf(ofDays(1), ofMinutes(1), ofNanos(5)))).assertContains("ld: [ 1 d, 1 m, 5 ns ]")
    }

    @Test
    fun testSerializeMapOfDuration() {
        Hocon.encodeToConfig(ConfigMap(mapOf("day" to ofDays(2), "hour" to ofHours(5), "minute" to ofMinutes(3))))
            .assertContains("mp: { day = 2 d, hour = 5 h, minute = 3 m }")
        Hocon.encodeToConfig(ConfigMapDurationKey(mapOf(ofHours(1) to ofSeconds(3600))))
            .assertContains("mp: { 1 h = 1 h }")
    }

    @Test
    fun testSerializeComplexDuration() {
        val obj = Complex(
            i = 6,
            s = Simple(ofMinutes(5)),
            n = Nullable(null),
            l = listOf(Simple(ofMinutes(1)), Simple(ofSeconds(2))),
            ln = listOf(Nullable(null), Nullable(ofHours(6))),
            f = true,
            ld = listOf(ofDays(1), ofMinutes(1), ofNanos(5)),
            mp = mapOf("day" to ofDays(2), "hour" to ofHours(5), "minute" to ofMinutes(3)),
            mpp = mapOf(ofHours(1) to ofSeconds(3600))
        )
        Hocon.encodeToConfig(obj)
            .assertContains("""
                i = 6
                s: { d = 5 m }
                n: { d = null }
                l: [ { d = 1 m }, { d = 2 s } ]
                ln: [ { d = null }, { d = 6 h } ]
                f = true
                ld: [ 1 d, 1 m, 5 ns ]
                mp: { day = 2 d, hour = 5 h, minute = 3 m }
                mpp: { 1 h = 1 h }
            """.trimIndent())
    }

    @Test
    fun testDeserializeNullableDuration() {
        var obj = deserializeConfig("d = null", Nullable.serializer())
        assertNull(obj.d)

        obj = deserializeConfig("d = 5 days", Nullable.serializer())
        assertEquals(ofDays(5), obj.d!!)
    }

    @Test
    fun testDeserializeListOfDuration() {
        val obj = deserializeConfig("ld: [ 1d, 1m, 5ns ]", ConfigList.serializer())
        assertEquals(listOf(ofDays(1), ofMinutes(1), ofNanos(5)), obj.ld)
    }

    @Test
    fun testDeserializeMapOfDuration() {
        val obj = deserializeConfig("""
             mp: { day = 2d, hour = 5 hours, minute = 3 minutes }
        """.trimIndent(), ConfigMap.serializer())
        assertEquals(mapOf("day" to ofDays(2), "hour" to ofHours(5), "minute" to ofMinutes(3)), obj.mp)

        val objDurationKey = deserializeConfig("""
             mp: { 1 hour = 3600s }
        """.trimIndent(), ConfigMapDurationKey.serializer())
        assertEquals(mapOf(ofHours(1) to ofSeconds(3600)), objDurationKey.mp)
    }

    @Test
    fun testDeserializeComplexDuration() {
        val obj = deserializeConfig("""
            i = 6
            s: { d = 5m }
            n: { d = null }
            l: [ { d = 1m }, { d = 2s } ]
            ln: [ { d = null }, { d = 6h } ]
            f = true
            ld: [ 1d, 1m, 5ns ]
            mp: { day = 2d, hour = 5 hours, minute = 3 minutes }
            mpp: { 1 hour = 3600s }
        """.trimIndent(), Complex.serializer())
        assertEquals(ofMinutes(5), obj.s.d)
        assertNull(obj.n.d)
        assertEquals(listOf(Simple(ofMinutes(1)), Simple(ofSeconds(2))), obj.l)
        assertEquals(listOf(Nullable(null), Nullable(ofHours(6))), obj.ln)
        assertEquals(6, obj.i)
        assertTrue(obj.f)
        assertEquals(listOf(ofDays(1), ofMinutes(1), ofNanos(5)), obj.ld)
        assertEquals(mapOf("day" to ofDays(2), "hour" to ofHours(5), "minute" to ofMinutes(3)), obj.mp)
        assertEquals(mapOf(ofHours(1) to ofSeconds(3600)), obj.mpp)
    }

    @Test
    fun testThrowsWhenNotTimeUnitHocon() {
        val message = "Value at d cannot be read as Duration because it is not a valid HOCON duration value"
        assertFailsWith<SerializationException>(message) {
            deserializeConfig("d = 10 unknown", Simple.serializer())
        }
    }
}
