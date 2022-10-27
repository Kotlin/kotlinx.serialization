package kotlinx.serialization.hocon

import kotlin.test.*
import kotlin.time.*
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.*
import org.junit.Assert.*
import org.junit.Test

class HoconDurationDeserializerTest {

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
    fun testDeserializeDurationInHoconFormat() {
        var obj = deserializeConfig("d = 10s", Simple.serializer())
        assertEquals(10.seconds, obj.d)
        obj = deserializeConfig("d = 10 hours", Simple.serializer())
        assertEquals(10.hours, obj.d)
        obj = deserializeConfig("d = 5 ms", Simple.serializer())
        assertEquals(5.milliseconds, obj.d)
    }

    @Test
    fun testDeserializeNullableDurationInHoconFormat() {
        var obj = deserializeConfig("d = null", Nullable.serializer())
        assertNull(obj.d)

        obj = deserializeConfig("d = 5 days", Nullable.serializer())
        assertEquals(5.days, obj.d!!)
    }

    @Test
    fun testDeserializeListOfDurationInHoconFormat() {
        val obj = deserializeConfig("ld: [ 1d, 1m, 5ns ]", ConfigList.serializer())
        assertEquals(listOf(1.days, 1.minutes, 5.nanoseconds), obj.ld)
    }

    @Test
    fun testDeserializeMapOfDurationInHoconFormat() {
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
    fun testDeserializeComplexDurationInHoconFormat() {
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
        val message = "Value at d cannot be read as kotlin.Duration because it is not a valid HOCON duration value"
        assertFailsWith<SerializationException>(message) {
            deserializeConfig("d = 10 unknown", Simple.serializer())
        }
    }
}
