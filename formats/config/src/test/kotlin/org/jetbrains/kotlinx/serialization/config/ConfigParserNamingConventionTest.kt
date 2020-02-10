/*
 * Copyright 2018-2020 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlinx.serialization.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Test

class ConfigParserNamingConventionTest {

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
