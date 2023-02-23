package kotlinx.serialization.hocon

import kotlin.test.*
import kotlin.time.*
import kotlin.time.Duration.Companion.INFINITE
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.*
import org.junit.Assert.*
import org.junit.Test

class HoconDurationTest {

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

    @Test
    fun testSerializeDuration() {
        Hocon.encodeToConfig(Simple(10.minutes)).assertContains("d = 10 m")
        Hocon.encodeToConfig(Simple(120.seconds)).assertContains("d = 2 m")

        Hocon.encodeToConfig(Simple(1.hours)).assertContains("d = 1 h")
        Hocon.encodeToConfig(Simple(120.minutes)).assertContains("d = 2 h")
        Hocon.encodeToConfig(Simple((3600 * 3).seconds)).assertContains("d = 3 h")

        Hocon.encodeToConfig(Simple(3.days)).assertContains("d = 3 d")
        Hocon.encodeToConfig(Simple(24.hours)).assertContains("d = 1 d")
        Hocon.encodeToConfig(Simple((1440 * 2).minutes)).assertContains("d = 2 d")
        Hocon.encodeToConfig(Simple((86400 * 4).seconds)).assertContains("d = 4 d")

        Hocon.encodeToConfig(Simple(1.seconds)).assertContains("d = 1 s")
        Hocon.encodeToConfig(Simple(2.minutes + 1.seconds)).assertContains("d = 121 s")
        Hocon.encodeToConfig(Simple(1.hours + 1.seconds)).assertContains("d = 3601 s")
        Hocon.encodeToConfig(Simple(1.days + 5.seconds)).assertContains("d = 86405 s")

        Hocon.encodeToConfig(Simple(9.nanoseconds)).assertContains("d = 9 ns")
        Hocon.encodeToConfig(Simple(1_000_000.nanoseconds + 5.seconds)).assertContains("d = 5001 ms")
        Hocon.encodeToConfig(Simple(1_000.nanoseconds + 9.seconds)).assertContains("d = 9000001 us")
        Hocon.encodeToConfig(Simple(1_000_005.nanoseconds + 5.seconds)).assertContains("d = 5001000005 ns")
        Hocon.encodeToConfig(Simple(1_002.nanoseconds + 9.seconds)).assertContains("d = 9000001002 ns")
        Hocon.encodeToConfig(Simple(1_000_000_001.nanoseconds)).assertContains("d = 1000000001 ns")

        // for INFINITE nanoseconds=0
        Hocon.encodeToConfig(Simple(INFINITE)).assertContains("d = ${Long.MAX_VALUE} s")
        Hocon.encodeToConfig(Simple(Long.MAX_VALUE.days)).assertContains("d = ${Long.MAX_VALUE} s")

        Hocon.encodeToConfig(Simple((-10).days)).assertContains("d = -10 d")
    }

    @Test
    fun testSerializeNullableDuration() {
        Hocon.encodeToConfig(Nullable(null)).assertContains("d = null")
        Hocon.encodeToConfig(Nullable(6.seconds)).assertContains("d = 6 s")
    }

    @Test
    fun testSerializeListOfDuration() {
        Hocon.encodeToConfig(ConfigList(listOf(1.days, 1.minutes, 5.nanoseconds))).assertContains("ld: [ 1 d, 1 m, 5 ns ]")
    }

    @Test
    fun testSerializeMapOfDuration() {
        Hocon.encodeToConfig(ConfigMap(mapOf("day" to 2.days, "hour" to 5.hours, "minute" to 3.minutes)))
            .assertContains("mp: { day = 2 d, hour = 5 h, minute = 3 m }")
        Hocon.encodeToConfig(ConfigMapDurationKey(mapOf(1.hours to 3600.seconds)))
            .assertContains("mp: { 1 h = 1 h }")
    }

    @Test
    fun testSerializeComplexDuration() {
        val obj = Complex(
            i = 6,
            s = Simple(5.minutes),
            n = Nullable(null),
            l = listOf(Simple(1.minutes), Simple(2.seconds)),
            ln = listOf(Nullable(null), Nullable(6.hours)),
            f = true,
            ld = listOf(1.days, 1.minutes, 5.nanoseconds),
            mp = mapOf("day" to 2.days, "hour" to 5.hours, "minute" to 3.minutes),
            mpp = mapOf(1.hours to 3600.seconds)
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
        assertEquals(10.seconds, obj.d)
        obj = deserializeConfig("d = 10 hours", Simple.serializer())
        assertEquals(10.hours, obj.d)
        obj = deserializeConfig("d = 5 ms", Simple.serializer())
        assertEquals(5.milliseconds, obj.d)
        obj = deserializeConfig("d = -5 days", Simple.serializer())
        assertEquals((-5).days, obj.d)
    }

    @Test
    fun testDeserializeNullableDuration() {
        var obj = deserializeConfig("d = null", Nullable.serializer())
        assertNull(obj.d)

        obj = deserializeConfig("d = 5 days", Nullable.serializer())
        assertEquals(5.days, obj.d!!)
    }

    @Test
    fun testDeserializeListOfDuration() {
        val obj = deserializeConfig("ld: [ 1d, 1m, 5ns ]", ConfigList.serializer())
        assertEquals(listOf(1.days, 1.minutes, 5.nanoseconds), obj.ld)
    }

    @Test
    fun testDeserializeMapOfDuration() {
        val obj = deserializeConfig("""
             mp: { day = 2d, hour = 5 hours, minute = 3 minutes }
        """.trimIndent(), ConfigMap.serializer())
        assertEquals(mapOf("day" to 2.days, "hour" to 5.hours, "minute" to 3.minutes), obj.mp)

        val objDurationKey = deserializeConfig("""
             mp: { 1 hour = 3600s }
        """.trimIndent(), ConfigMapDurationKey.serializer())
        assertEquals(mapOf(1.hours to 3600.seconds), objDurationKey.mp)
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
        assertEquals(5.minutes, obj.s.d)
        assertNull(obj.n.d)
        assertEquals(listOf(Simple(1.minutes), Simple(2.seconds)), obj.l)
        assertEquals(listOf(Nullable(null), Nullable(6.hours)), obj.ln)
        assertEquals(6, obj.i)
        assertTrue(obj.f)
        assertEquals(listOf(1.days, 1.minutes, 5.nanoseconds), obj.ld)
        assertEquals(mapOf("day" to 2.days, "hour" to 5.hours, "minute" to 3.minutes), obj.mp)
        assertEquals(mapOf(1.hours to 3600.seconds), obj.mpp)
    }

    @Test
    fun testThrowsWhenNotTimeUnitHocon() {
        val message = "Value at d cannot be read as Duration because it is not a valid HOCON duration value"
        assertFailsWith<SerializationException>(message) {
            deserializeConfig("d = 10 unknown", Simple.serializer())
        }
    }
}
