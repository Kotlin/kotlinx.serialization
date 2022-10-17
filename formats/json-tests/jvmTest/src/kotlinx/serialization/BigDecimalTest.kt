package kotlinx.serialization

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import org.junit.Test
import java.math.BigDecimal

private typealias BigDecimalKxs = @Serializable(with = BigDecimalNumericSerializer::class) BigDecimal

class BigDecimalTest : JsonTestBase() {

    private val json = Json {
        prettyPrint = true
    }

    private inline fun <reified T> assertBigDecimalJsonFormAndRestored(
        expected: String,
        actual: T,
        serializer: KSerializer<T> = serializer(),
    ) = assertJsonFormAndRestored(
        serializer,
        actual,
        expected,
        json
    )

    @Test
    fun bigDecimal() {
        fun test(expected: String, actual: BigDecimal) =
            assertBigDecimalJsonFormAndRestored(expected, actual, BigDecimalNumericSerializer)

        test("0", BigDecimal.ZERO)
        test("1", BigDecimal.ONE)
        test("-1", BigDecimal("-1"))
        test("10", BigDecimal.TEN)
        test(bdExpected1, bdActual1)
        test(bdExpected2, bdActual2)
        test(bdExpected3, bdActual3)
        test(bdExpected4, bdActual4)
        test(bdExpected5, bdActual5)
        test(bdExpected6, bdActual6)
    }

    @Test
    fun bigDecimalList() {

        val bdList: List<BigDecimal> = listOf(
            bdActual1,
            bdActual2,
            bdActual3,
            bdActual4,
            bdActual5,
            bdActual6,
        )

        val expected =
            """
                [
                    $bdExpected1,
                    $bdExpected2,
                    $bdExpected3,
                    $bdExpected4,
                    $bdExpected5,
                    $bdExpected6
                ]
            """.trimIndent()

        assertJsonFormAndRestored(
            ListSerializer(BigDecimalNumericSerializer),
            bdList,
            expected,
            json,
        )
    }

    @Test
    fun bigDecimalMap() {
        val bdMap: Map<BigDecimal, BigDecimal> = mapOf(
            bdActual1 to bdActual2,
            bdActual3 to bdActual4,
            bdActual5 to bdActual6,
        )

        val expected =
            """
                {
                    "$bdExpected1": $bdExpected2,
                    "$bdExpected3": $bdExpected4,
                    "$bdExpected5": $bdExpected6
                }
            """.trimIndent()

        assertJsonFormAndRestored(
            MapSerializer(BigDecimalNumericSerializer, BigDecimalNumericSerializer),
            bdMap,
            expected,
            json,
        )
    }

    @Test
    fun bigDecimalHolder() {
        val bdHolder = BigDecimalHolder(
            bd = bdActual1,
            bdList = listOf(
                bdActual1,
                bdActual2,
                bdActual3,
            ),
            bdMap = mapOf(
                bdActual1 to bdActual2,
                bdActual3 to bdActual4,
                bdActual5 to bdActual6,
            ),
        )

        val expected =
            """
                {
                    "bd": $bdExpected1,
                    "bdList": [
                        $bdExpected1,
                        $bdExpected2,
                        $bdExpected3
                    ],
                    "bdMap": {
                        "$bdExpected1": $bdExpected2,
                        "$bdExpected3": $bdExpected4,
                        "$bdExpected5": $bdExpected6
                    }
                }
            """.trimIndent()

        assertBigDecimalJsonFormAndRestored(
            expected,
            bdHolder,
        )
    }

    companion object {

        // test data
        private val bdActual1 = BigDecimal("725345854747326287606413621318.311864440287151714280387858224")
        private val bdActual2 = BigDecimal("336052472523017262165484244513.836582112201211216526831524328")
        private val bdActual3 = BigDecimal("211054843014778386028147282517.011200287614476453868782405400")
        private val bdActual4 = BigDecimal("364751025728628060231208776573.207325218263752602211531367642")
        private val bdActual5 = BigDecimal("508257556021513833656664177125.824502734715222686411316853148")
        private val bdActual6 = BigDecimal("127134584027580606401102614002.366672301517071543257300444000")

        private const val bdExpected1 = "725345854747326287606413621318.311864440287151714280387858224"
        private const val bdExpected2 = "336052472523017262165484244513.836582112201211216526831524328"
        private const val bdExpected3 = "211054843014778386028147282517.011200287614476453868782405400"
        private const val bdExpected4 = "364751025728628060231208776573.207325218263752602211531367642"
        private const val bdExpected5 = "508257556021513833656664177125.824502734715222686411316853148"
        private const val bdExpected6 = "127134584027580606401102614002.366672301517071543257300444000"
    }

}

@Serializable
private data class BigDecimalHolder(
    val bd: BigDecimalKxs,
    val bdList: List<BigDecimalKxs>,
    val bdMap: Map<BigDecimalKxs, BigDecimalKxs>,
)

private object BigDecimalNumericSerializer : KSerializer<BigDecimal> {

    override val descriptor = PrimitiveSerialDescriptor("java.math.BigDecimal", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): BigDecimal {
        return if (decoder is JsonDecoder) {
            BigDecimal(decoder.decodeJsonElement().jsonPrimitive.content)
        } else {
            BigDecimal(decoder.decodeString())
        }
    }

    override fun serialize(encoder: Encoder, value: BigDecimal) {
        val bdString = value.toPlainString()

        if (encoder is JsonEncoder) {
            encoder.encodeJsonElement(JsonUnquotedLiteral(bdString))
        } else {
            encoder.encodeString(bdString)
        }
    }
}
