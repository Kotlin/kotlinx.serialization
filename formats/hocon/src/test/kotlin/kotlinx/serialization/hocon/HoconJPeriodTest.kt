@file:UseSerializers(JPeriodSerializer::class)
package kotlinx.serialization.hocon

import java.time.Period
import java.time.Period.*
import kotlinx.serialization.*
import kotlinx.serialization.hocon.serializers.JPeriodSerializer
import org.junit.Assert.*
import org.junit.Test
import kotlin.test.assertFailsWith

class HoconJPeriodTest {

    @Serializable
    data class Simple(val d: Period)

    @Serializable
    data class Nullable(val d: Period?)

    @Serializable
    data class ConfigList(val ld: List<Period>)

    @Serializable
    data class ConfigMap(val mp: Map<String, Period>)

    @Serializable
    data class ConfigMapPeriodKey(val mp: Map<Period, Period>)

    @Serializable
    data class Complex(
        val i: Int,
        val s: Simple,
        val n: Nullable,
        val l: List<Simple>,
        val ln: List<Nullable>,
        val f: Boolean,
        val ld: List<Period>,
        val mp: Map<String, Period>,
        val mpp: Map<Period, Period>
    )

    @Test
    fun testSerializePeriod() {
        Hocon.encodeToConfig(Simple(ofDays(360))).assertContains("d = 360 d")
        Hocon.encodeToConfig(Simple(ofMonths(5))).assertContains("d = 5 m")
        Hocon.encodeToConfig(Simple(ofWeeks(-6))).assertContains("d = -42 d")
        Hocon.encodeToConfig(Simple(ofYears(3))).assertContains("d = 3 y")
        Hocon.encodeToConfig(Simple(of(2, 6, 0))).assertContains("d = 30 m")
        Hocon.encodeToConfig(Simple(ofMonths(24))).assertContains("d = 2 y")
        Hocon.encodeToConfig(Simple(ofMonths(25))).assertContains("d = 25 m")
    }

    @Test
    fun testThrowsWhenSeveralTimeUnits() {
        val message = "Not possible to serialize java.time.Period because only one time unit can be specified in HOCON"
        assertFailsWith<SerializationException>(message) {
            Hocon.encodeToConfig(Simple(of(2, 6, 5)))
        }
    }

    @Test
    fun testSerializeNullablePeriod() {
        Hocon.encodeToConfig(Nullable(null)).assertContains("d = null")
        Hocon.encodeToConfig(Nullable(ofMonths(5))).assertContains("d = 5 m")
    }

    @Test
    fun testSerializeListOfPeriod() {
        Hocon.encodeToConfig(ConfigList(listOf(ofDays(1), ofMonths(1), ofYears(5))))
            .assertContains("ld: [ 1 d, 1 m, 5 y ]")
    }

    @Test
    fun testSerializeMapOfPeriod() {
        Hocon.encodeToConfig(ConfigMap(mapOf("day" to ofDays(2), "year" to ofYears(5), "month" to ofMonths(3))))
            .assertContains("mp: { day = 2 d, year = 5 y, month = 3 m }")
        Hocon.encodeToConfig(ConfigMapPeriodKey(mapOf(ofYears(1) to ofMonths(12))))
            .assertContains("mp: { 1 y = 1 y }")
    }

    @Test
    fun testSerializeComplexPeriod() {
        val obj = Complex(
            i = 6,
            s = Simple(ofMonths(5)),
            n = Nullable(null),
            l = listOf(Simple(ofMonths(1)), Simple(ofDays(2))),
            ln = listOf(Nullable(null), Nullable(ofDays(6))),
            f = true,
            ld = listOf(ofDays(1), ofYears(1), ofMonths(5)),
            mp = mapOf("day" to ofDays(2), "year" to ofYears(5), "month" to ofMonths(3)),
            mpp = mapOf(ofYears(1) to ofMonths(16))
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
                mp: { day = 2 d, year = 5 y, month = 3 m }
                mpp: { 1 y = 16 m }
            """.trimIndent())
    }

    @Test
    fun testDeserializePeriod() {
        var obj = deserializeConfig("d = 10 d", Simple.serializer())
        assertEquals(ofDays(10), obj.d)

        obj = deserializeConfig("d = 2 w", Simple.serializer())
        assertEquals(ofWeeks(2), obj.d)

        obj = deserializeConfig("d = 18 m", Simple.serializer())
        assertEquals(ofMonths(18), obj.d)

        obj = deserializeConfig("d = 6 y", Simple.serializer())
        assertEquals(ofYears(6), obj.d)
    }

    @Test
    fun testDeserializeNullablePeriod() {
        var obj = deserializeConfig("d = null", Nullable.serializer())
        assertNull(obj.d)

        obj = deserializeConfig("d = 18 m", Nullable.serializer())
        assertEquals(ofMonths(18), obj.d)
    }

    @Test
    fun testDeserializeListOfPeriod() {
        val obj = deserializeConfig("ld: [ 1d, 1m, 5y ]", ConfigList.serializer())
        assertEquals(listOf(ofDays(1), ofMonths(1), ofYears(5)), obj.ld)
    }

    @Test
    fun testDeserializeMapOfPeriod() {
        val obj = deserializeConfig("""
             mp: { day = 2d, year = 5 y, month = 3 months }
        """.trimIndent(), ConfigMap.serializer())
        assertEquals(mapOf("day" to ofDays(2), "year" to ofYears(5), "month" to ofMonths(3)), obj.mp)

        val objDurationKey = deserializeConfig("""
             mp: { 1 year = 12 months }
        """.trimIndent(), ConfigMapPeriodKey.serializer())
        assertEquals(mapOf(ofYears(1) to ofMonths(12)), objDurationKey.mp)
    }

    @Test
    fun testDeserializeComplexPeriod() {
        val obj = deserializeConfig("""
            i = 6
            s: { d = 5m }
            n: { d = null }
            l: [ { d = 1m }, { d = 2y } ]
            ln: [ { d = null }, { d = 6d } ]
            f = true
            ld: [ 1d, 1m, 5y ]
            mp: { day = 2d, year = 5 y, month = 3 months }
            mpp: { 1 year = 12 months }
        """.trimIndent(), Complex.serializer())
        assertEquals(ofMonths(5), obj.s.d)
        assertNull(obj.n.d)
        assertEquals(listOf(Simple(ofMonths(1)), Simple(ofYears(2))), obj.l)
        assertEquals(listOf(Nullable(null), Nullable(ofDays(6))), obj.ln)
        assertEquals(6, obj.i)
        assertTrue(obj.f)
        assertEquals(listOf(ofDays(1), ofMonths(1), ofYears(5)), obj.ld)
        assertEquals(mapOf("day" to ofDays(2), "year" to ofYears(5), "month" to ofMonths(3)), obj.mp)
        assertEquals(mapOf(ofYears(1) to ofMonths(12)), obj.mpp)
    }

    @Test
    fun testThrowsWhenNotPeriodFormatHocon() {
        val message = "Value at d cannot be read as java.time.Period because it is not a valid HOCON Period Format value"
        assertFailsWith<SerializationException>(message) {
            deserializeConfig("d = 10 unknown", Simple.serializer())
        }
    }
}
