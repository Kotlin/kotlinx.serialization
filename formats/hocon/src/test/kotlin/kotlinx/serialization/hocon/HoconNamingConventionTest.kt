/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.hocon

import kotlinx.serialization.*
import org.junit.*
import org.junit.Assert.*

class HoconNamingConventionTest {

    @Serializable
    data class CaseConfig(val aCharValue: Char, val aStringValue: String)

    @Serializable
    data class HoconNameConfig(@HoconName("anID-value") val anIDValue: Int)

    @Serializable
    data class CaseWithInnerConfig(val caseConfig: CaseConfig, val hoconNameConfig: HoconNameConfig)

    private val hocon = Hocon {
        useConfigNamingConvention = true
    }

    @Test
    fun testDeserializeUsingNamingConvention() {
        val obj = deserializeConfig("a-char-value = t, a-string-value = test", CaseConfig.serializer(), true)
        assertEquals('t', obj.aCharValue)
        assertEquals("test", obj.aStringValue)
    }

    @Test
    fun testSerializeUsingNamingConvention() {
        val obj = CaseConfig(aCharValue = 't', aStringValue = "test")
        val config = hocon.encodeToConfig(obj)

        config.assertContains("a-char-value = t, a-string-value = test")
    }

    @Test
    fun testDeserializeUsingHoconNameInsteadOfNamingConvention() {
        val obj = deserializeConfig("anID-value = 42", HoconNameConfig.serializer(), true)
        assertEquals(42, obj.anIDValue)
    }

    @Test
    fun testSerializeUsingHoconNameInsteadOfNamingConvention() {
        val obj = HoconNameConfig(anIDValue = 42)
        val config = hocon.encodeToConfig(obj)

        config.assertContains("anID-value = 42")
    }

    @Test
    fun testDeserializeInnerValuesUsingNamingConvention() {
        val configString = "case-config {a-char-value = b, a-string-value = bar}, hocon-name-config {anID-value = 21}"
        val obj = deserializeConfig(configString, CaseWithInnerConfig.serializer(), true)
        with(obj.caseConfig) {
            assertEquals('b', aCharValue)
            assertEquals("bar", aStringValue)
        }
        assertEquals(21, obj.hoconNameConfig.anIDValue)
    }

    @Test
    fun testSerializeInnerValuesUsingNamingConvention() {
        val obj = CaseWithInnerConfig(
            caseConfig = CaseConfig(aCharValue = 't', aStringValue = "test"),
            hoconNameConfig = HoconNameConfig(anIDValue = 42)
        )
        val config = hocon.encodeToConfig(obj)

        config.assertContains(
            """
                case-config { a-char-value = t, a-string-value = test }
                hocon-name-config { anID-value = 42 }
            """
        )
    }
}
