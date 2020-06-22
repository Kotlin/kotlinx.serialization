/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.json.Json
import kotlin.test.*

class ClassWithMultipleMasksTest {

    /*
     * Plugin generates int mask for each 32 fields.
     * This test ensures that mask is properly generated when fields count is greater than 32.
     */
    @Serializable
    data class BigDummyData(
        val regular: String,
        @SerialName("field0") val field0: String? = null,
        @SerialName("field1") val field1: String? = null,
        @SerialName("field2") val field2: String? = null,
        @SerialName("field3") val field3: String? = null,
        @SerialName("field4") val field4: String? = null,
        @SerialName("field5") val field5: String? = null,
        @SerialName("field6") val field6: String? = null,
        @SerialName("field7") val field7: String? = null,
        @SerialName("field8") val field8: String? = null,
        @SerialName("field9") val field9: String? = null,
        @SerialName("field10") val field10: String? = null,
        @SerialName("field11") val field11: String? = null,
        @SerialName("field12") val field12: String? = null,
        @SerialName("field13") val field13: String? = null,
        @SerialName("field14") val field14: String? = null,
        @SerialName("field15") val field15: String? = null,
        @SerialName("field16") val field16: String? = null,
        @SerialName("field17") val field17: String? = null,
        @SerialName("field18") val field18: String? = null,
        @SerialName("field19") val field19: String? = null,
        @SerialName("field20") val field20: String? = null,
        @SerialName("field21") val field21: String? = null,
        @SerialName("field22") val field22: String? = null,
        @SerialName("field23") val field23: String? = null,
        @SerialName("field24") val field24: String? = null,
        @SerialName("field25") val field25: String? = null,
        @SerialName("field26") val field26: String? = null,
        @SerialName("field27") val field27: String? = null,
        @SerialName("field28") val field28: String? = null,
        @SerialName("field29") val field29: String? = null,
        @SerialName("field30") val field30: String? = null,
        @SerialName("field31") val field31: String? = null,
        @SerialName("field32") val field32: String? = null,
        @SerialName("field33") val field33: String? = null,
        @SerialName("field34") val field34: String? = null,
        @SerialName("field35") val field35: String? = null,
        @SerialName("field36") val field36: String? = null,
        @SerialName("field37") val field37: String? = null,
        @SerialName("field38") val field38: String? = null,
        @SerialName("field39") val field39: String? = null,
        @SerialName("field40") val field40: String? = "b",
        @Required val requiredLast: String = "required"
    )

    @Test
    fun testMoreThan32Fields() {
        val data = BigDummyData("a")
        val message = Json.encodeToString(BigDummyData.serializer(), data)
        println(message)
        val restored = Json.decodeFromString(BigDummyData.serializer(), """{"regular": "0","requiredLast":"r"}""")
        with(restored) {
            assertEquals("0", regular)
            assertEquals("b", field40)
            assertEquals(null, field39)
            assertEquals("r", requiredLast)
        }

        val restored2 = Json.decodeFromString(BigDummyData.serializer(), """{"regular": "0", "field39":"f","requiredLast":"required"}""")
        with(restored2) {
            assertEquals("0", regular)
            assertEquals("b", field40)
            assertEquals("f", field39)
            assertEquals("required", requiredLast)
        }
        assertFailsWith<MissingFieldException> { Json.decodeFromString(BigDummyData.serializer(), """{"regular": "0"}""") }
    }
}
