package kotlinx.benchmarks.json

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(2)
open class ImplicitNullsBenchmark {

    @Serializable
    data class Values(
        val field0: Int?,
        val field1: Int?,
        val field2: Int?,
        val field3: Int?,
        val field4: Int?,
        val field5: Int?,
        val field6: Int?,
        val field7: Int?,
        val field8: Int?,
        val field9: Int?,

        val field10: Int?,
        val field11: Int?,
        val field12: Int?,
        val field13: Int?,
        val field14: Int?,
        val field15: Int?,
        val field16: Int?,
        val field17: Int?,
        val field18: Int?,
        val field19: Int?,

        val field20: Int?,
        val field21: Int?,
        val field22: Int?,
        val field23: Int?,
        val field24: Int?,
        val field25: Int?,
        val field26: Int?,
        val field27: Int?,
        val field28: Int?,
        val field29: Int?,

        val field30: Int?,
        val field31: Int?
    )


    private val jsonImplicitNulls = Json { explicitNulls = false }

    private val valueWithNulls = Values(
        null, null, 2, null, null, null, null, null, null, null,
        null, null, null, null, 14, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null,
        null, null
    )


    private val jsonWithNulls = """{"field0":null,"field1":null,"field2":2,"field3":null,"field4":null,"field5":null,
        |"field6":null,"field7":null,"field8":null,"field9":null,"field10":null,"field11":null,"field12":null,
        |"field13":null,"field14":14,"field15":null,"field16":null,"field17":null,"field18":null,"field19":null,
        |"field20":null,"field21":null,"field22":null,"field23":null,"field24":null,"field25":null,"field26":null,
        |"field27":null,"field28":null,"field29":null,"field30":null,"field31":null}""".trimMargin()

    private val jsonNoNulls = """{"field0":0,"field1":1,"field2":2,"field3":3,"field4":4,"field5":5,
        |"field6":6,"field7":7,"field8":8,"field9":9,"field10":10,"field11":11,"field12":12,
        |"field13":13,"field14":14,"field15":15,"field16":16,"field17":17,"field18":18,"field19":19,
        |"field20":20,"field21":21,"field22":22,"field23":23,"field24":24,"field25":25,"field26":26,
        |"field27":27,"field28":28,"field29":29,"field30":30,"field31":31}""".trimMargin()

    private val jsonWithAbsence = """{"field2":2, "field14":14}"""

    private val serializer = Values.serializer()

    @Benchmark
    fun decodeNoNulls() {
        Json.decodeFromString(serializer, jsonNoNulls)
    }

    @Benchmark
    fun decodeNoNullsImplicit() {
        jsonImplicitNulls.decodeFromString(serializer, jsonNoNulls)
    }

    @Benchmark
    fun decodeNulls() {
        Json.decodeFromString(serializer, jsonWithNulls)
    }

    @Benchmark
    fun decodeNullsImplicit() {
        jsonImplicitNulls.decodeFromString(serializer, jsonWithNulls)
    }

    @Benchmark
    fun decodeAbsenceImplicit() {
        jsonImplicitNulls.decodeFromString(serializer, jsonWithAbsence)
    }

    @Benchmark
    fun encodeNulls() {
        Json.encodeToString(serializer, valueWithNulls)
    }

    @Benchmark
    fun encodeNullsImplicit() {
        jsonImplicitNulls.encodeToString(serializer, valueWithNulls)
    }
}
