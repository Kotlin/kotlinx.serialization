@file:UseSerializers(JTemporalAmountSerializer::class)
package kotlinx.serialization.hocon

import kotlinx.serialization.*
import kotlinx.serialization.hocon.serializers.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*
import java.time.chrono.JapaneseChronology
import java.time.temporal.TemporalAmount
import kotlin.test.assertFailsWith

class HoconJTemporalAmountTest {

    @Serializable
    data class Simple(val d: TemporalAmount)

    @Serializable
    data class Nullable(val d: TemporalAmount?)

    @Serializable
    data class ConfigList(val ld: List<TemporalAmount>)

    @Serializable
    data class ConfigMap(val mp: Map<String, TemporalAmount>)

    @Serializable
    data class ConfigMapTemporalKey(val mp: Map<TemporalAmount, TemporalAmount>)

    @Serializable
    data class Complex(
        val i: Int,
        val s: Simple,
        val n: Nullable,
        val l: List<Simple>,
        val ln: List<Nullable>,
        val f: Boolean,
        val ld: List<TemporalAmount>,
        val mp: Map<String, TemporalAmount>,
        val mpp: Map<TemporalAmount, TemporalAmount>
    )

    @Test
    fun testSerializeTemporalAmount() {
        Hocon.encodeToConfig(Simple(Period.ofYears(10))).assertContains("d = 10 y")
        Hocon.encodeToConfig(Simple(Period.ofMonths(5))).assertContains("d = 5 m")
        Hocon.encodeToConfig(Simple(Duration.ofHours(6))).assertContains("d = 6 h")
        Hocon.encodeToConfig(Simple(Duration.ofMinutes(6))).assertContains("d = 6 m")
    }

    @Test
    fun testThrowsWhenOtherImplementations() {
        val temporal = JapaneseChronology.INSTANCE.period(10, 12, 13)
        val message = "Class ${temporal::class.java} cannot be serialized in Hocon"
        assertFailsWith<SerializationException>(message) {
            Hocon.encodeToConfig(Simple(temporal))
        }
    }

    @Test
    fun testSerializeNullableTemporalAmount() {
        Hocon.encodeToConfig(Nullable(null)).assertContains("d = null")
        Hocon.encodeToConfig(Nullable(Duration.ofHours(6))).assertContains("d = 6 h")
        Hocon.encodeToConfig(Nullable(Period.ofYears(10))).assertContains("d = 10 y")
    }

    @Test
    fun testSerializeListOfTemporalAmount() {
        Hocon.encodeToConfig(ConfigList(listOf(Period.ofDays(1), Duration.ofNanos(10))))
            .assertContains("ld: [ 1 d, 10 ns]")
    }

    @Test
    fun testSerializeMapOfTemporalAmount() {
        Hocon.encodeToConfig(ConfigMap(mapOf("period" to Period.ofDays(1), "duration" to Duration.ofSeconds(1))))
            .assertContains("mp: { period = 1 d, duration = 1 s }")
        Hocon.encodeToConfig(ConfigMapTemporalKey(mapOf(Duration.ofSeconds(10) to Period.ofDays(1))))
            .assertContains("mp: { 10 s = 1 d }")
    }

    @Test
    fun testSerializeComplexTemporalAmount() {
        val obj = Complex(
            i = 6,
            s = Simple(Period.ofMonths(5)),
            n = Nullable(null),
            l = listOf(Simple(Duration.ofMinutes(1)), Simple(Period.ofDays(2))),
            ln = listOf(Nullable(null), Nullable(Duration.ofDays(6))),
            f = true,
            ld = listOf(Duration.ofDays(1), Period.ofYears(1), Period.ofMonths(5)),
            mp = mapOf("day" to Period.ofDays(2), "year" to Period.ofYears(5), "minutes" to Duration.ofMinutes(3)),
            mpp = mapOf(Duration.ofNanos(1) to Period.ofMonths(16))
        )
        Hocon.encodeToConfig(obj)
            .assertContains("""
                i = 6
                s: { d = 5 m }
                n: { d = null }
                l: [ { d = 1 m }, { d = 2 d } ]
                ln: [ { d = null }, { d = 6 d } ]
                f = true
                ld: [ 1 d, 1 y, 5 m ]
                mp: { day = 2 d, year = 5 y, minutes = 3 m }
                mpp: { 1 ns = 16 m }
            """.trimIndent())
    }

    @Test
    fun testDeserializeTemporalAmount() {
        var obj = deserializeConfig("d = 10 d", Simple.serializer())
        assertEquals(Duration.ofDays(10), obj.d)

        obj = deserializeConfig("d = 10 y", Simple.serializer())
        assertEquals(Period.ofYears(10), obj.d)

        obj = deserializeConfig("d = 18 m", Simple.serializer())
        assertEquals(Duration.ofMinutes(18), obj.d)
    }

    @Test
    fun testDeserializeNullableTemporalAmount() {
        var obj = deserializeConfig("d = null", Nullable.serializer())
        assertNull(obj.d)

        obj = deserializeConfig("d = 18 m", Nullable.serializer())
        assertEquals(Duration.ofMinutes(18), obj.d)

        obj = deserializeConfig("d = 10 y", Nullable.serializer())
        assertEquals(Period.ofYears(10), obj.d)
    }

    @Test
    fun testDeserializeListOfTemporalAmount() {
        val obj = deserializeConfig("ld: [ 1d, 1m, 5y ]", ConfigList.serializer())
        assertEquals(listOf(Duration.ofDays(1), Duration.ofMinutes(1), Period.ofYears(5)), obj.ld)
    }

    @Test
    fun testDeserializeMapOfTemporalAmount() {
        val obj = deserializeConfig("""
             mp: { day = 2 d, year = 5 y, seconds = 3 s }
        """.trimIndent(), ConfigMap.serializer())
        assertEquals(mapOf("day" to Duration.ofDays(2), "year" to Period.ofYears(5), "seconds" to Duration.ofSeconds(3)), obj.mp)

        val objTemporalKey = deserializeConfig("""
             mp: { 1 year = 12 seconds }
        """.trimIndent(), ConfigMapTemporalKey.serializer())
        assertEquals(mapOf(Period.ofYears(1) to Duration.ofSeconds(12)), objTemporalKey.mp)
    }

    @Test
    fun testDeserializeComplexTemporalAmount() {
        val obj = deserializeConfig("""
            i = 6
            s: { d = 5m }
            n: { d = null }
            l: [ { d = 1m }, { d = 2y } ]
            ln: [ { d = null }, { d = 6d } ]
            f = true
            ld: [ 1d, 1 month, 5y ]
            mp: { day = 2d, year = 5 y, month = 3 months }
            mpp: { 1 year = 12 months }
        """.trimIndent(), Complex.serializer())
        assertEquals(Duration.ofMinutes(5), obj.s.d)
        assertNull(obj.n.d)
        assertEquals(listOf(Simple(Duration.ofMinutes(1)), Simple(Period.ofYears(2))), obj.l)
        assertEquals(listOf(Nullable(null), Nullable(Duration.ofDays(6))), obj.ln)
        assertEquals(6, obj.i)
        assertTrue(obj.f)
        assertEquals(listOf(Duration.ofDays(1), Period.ofMonths(1), Period.ofYears(5)), obj.ld)
        assertEquals(mapOf("day" to Duration.ofDays(2), "year" to Period.ofYears(5), "month" to Period.ofMonths(3)), obj.mp)
        assertEquals(mapOf(Period.ofYears(1) to Period.ofMonths(12)), obj.mpp)
    }

    @Test
    fun testThrowsWhenInvalidFormatHocon() {
        val message = "Value at d cannot be read as java.time.temporal.TemporalAmount because it is not a valid HOCON value"
        assertFailsWith<SerializationException>(message) {
            deserializeConfig("d = 10 unknown", Simple.serializer())
        }
    }
}
