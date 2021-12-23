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
    data class SerialNameConfig(@SerialName("an-id-value") val anIDValue: Int)

    @Serializable
    data class CaseWithInnerConfig(val caseConfig: CaseConfig, val serialNameConfig: SerialNameConfig)

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
    fun testDeserializeUsingSerialNameInsteadOfNamingConvention() {
        val obj = deserializeConfig("an-id-value = 42", SerialNameConfig.serializer(), true)
        assertEquals(42, obj.anIDValue)
    }

    @Test
    fun testSerializeUsingSerialNameInsteadOfNamingConvention() {
        val obj = SerialNameConfig(anIDValue = 42)
        val config = hocon.encodeToConfig(obj)

        config.assertContains("an-id-value = 42")
    }

    @Test
    fun testDeserializeInnerValuesUsingNamingConvention() {
        val configString = "case-config {a-char-value = b, a-string-value = bar}, serial-name-config {an-id-value = 21}"
        val obj = deserializeConfig(configString, CaseWithInnerConfig.serializer(), true)
        with(obj.caseConfig) {
            assertEquals('b', aCharValue)
            assertEquals("bar", aStringValue)
        }
        assertEquals(21, obj.serialNameConfig.anIDValue)
    }

    @Test
    fun testSerializeInnerValuesUsingNamingConvention() {
        val obj = CaseWithInnerConfig(
            caseConfig = CaseConfig(aCharValue = 't', aStringValue = "test"),
            serialNameConfig = SerialNameConfig(anIDValue = 42)
        )
        val config = hocon.encodeToConfig(obj)

        config.assertContains(
            """
                case-config { a-char-value = t, a-string-value = test }
                serial-name-config { an-id-value = 42 }
            """
        )
    }
}
