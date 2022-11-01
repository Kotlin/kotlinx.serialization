@file:UseSerializers(JDurationSerializer::class)
package kotlinx.serialization.hocon

import java.time.Duration
import java.time.Duration.*
import kotlin.test.assertFailsWith
import kotlinx.serialization.*
import kotlinx.serialization.hocon.serializers.JDurationSerializer
import org.junit.*
import org.junit.Assert.*

class HoconJDurationTest {

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
        val ld: List<@Contextual Duration>,
        val mp: Map<String, @Contextual Duration>,
        val mpp: Map<@Contextual Duration, @Contextual Duration>
    )

    @Test
    fun testSerializeDuration() {
        Hocon.encodeToConfig(Simple(ofMinutes(10))).assertContains("d = 10 m")
        Hocon.encodeToConfig(Simple(ofSeconds(120))).assertContains("d = 2 m")

        Hocon.encodeToConfig(Simple(ofHours(1))).assertContains("d = 1 h")
        Hocon.encodeToConfig(Simple(ofMinutes(120))).assertContains("d = 2 h")
        Hocon.encodeToConfig(Simple(ofSeconds(3600 * 3))).assertContains("d = 3 h")

        Hocon.encodeToConfig(Simple(ofDays(3))).assertContains("d = 3 d")
        Hocon.encodeToConfig(Simple(ofHours(24))).assertContains("d = 1 d")
        Hocon.encodeToConfig(Simple(ofMinutes(1440 * 2))).assertContains("d = 2 d")
        Hocon.encodeToConfig(Simple(ofSeconds(86400 * 4))).assertContains("d = 4 d")

        Hocon.encodeToConfig(Simple(ofSeconds(1))).assertContains("d = 1 s")
        Hocon.encodeToConfig(Simple(ofMinutes(2).plusSeconds(1))).assertContains("d = 121 s")
        Hocon.encodeToConfig(Simple(ofHours(1).plusSeconds(1))).assertContains("d = 3601 s")
        Hocon.encodeToConfig(Simple(ofDays(1).plusSeconds(5))).assertContains("d = 86405 s")

        Hocon.encodeToConfig(Simple(ofNanos(9))).assertContains("d = 9 ns")
        Hocon.encodeToConfig(Simple(ofNanos(1_000_000).plusSeconds(5))).assertContains("d = 5001 ms")
        Hocon.encodeToConfig(Simple(ofNanos(1_000).plusSeconds(9))).assertContains("d = 9000001 us")
        Hocon.encodeToConfig(Simple(ofNanos(1_000_005).plusSeconds(5))).assertContains("d = 5001000005 ns")
        Hocon.encodeToConfig(Simple(ofNanos(1_002).plusSeconds(9))).assertContains("d = 9000001002 ns")
        Hocon.encodeToConfig(Simple(ofNanos(1_000_000_001))).assertContains("d = 1000000001 ns")

        Hocon.encodeToConfig(Simple(ofDays(-10))).assertContains("d = -10 d")
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
    fun testDeserializeDuration() {
        var obj = deserializeConfig("d = 10s", Simple.serializer())
        assertEquals(ofSeconds(10), obj.d)
        obj = deserializeConfig("d = 10 hours", Simple.serializer())
        assertEquals(ofHours(10), obj.d)
        obj = deserializeConfig("d = 5 ms", Simple.serializer())
        assertEquals(ofMillis(5), obj.d)
        obj = deserializeConfig("d = -5 days", Simple.serializer())
        assertEquals(ofDays(-5), obj.d)
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
