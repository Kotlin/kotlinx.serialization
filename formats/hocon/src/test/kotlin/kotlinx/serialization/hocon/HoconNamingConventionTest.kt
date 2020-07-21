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

    @Test
    fun `deserialize using naming convention`() {
        val obj = deserializeConfig("a-char-value = t, a-string-value = test", CaseConfig.serializer(), true)
        assertEquals('t', obj.aCharValue)
        assertEquals("test", obj.aStringValue)
    }

    @Test
    fun `use serial name instead of naming convention if provided`() {
        val obj = deserializeConfig("an-id-value = 42", SerialNameConfig.serializer(), true)
        assertEquals(42, obj.anIDValue)
    }

    @Test
    fun `deserialize inner values using naming convention`() {
        val configString = "case-config {a-char-value = b, a-string-value = bar}, serial-name-config {an-id-value = 21}"
        val obj = deserializeConfig(configString, CaseWithInnerConfig.serializer(), true)
        with(obj.caseConfig) {
            assertEquals('b', aCharValue)
            assertEquals("bar", aStringValue)
        }
        assertEquals(21, obj.serialNameConfig.anIDValue)
    }
}
