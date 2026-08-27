/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.json.JsonConfiguration
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.memberProperties
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ImmutableConfigurationTest {
    @Test
    fun testJsonConfigurationDoesNotHaveVars() {
        val mutableProperties =
            JsonConfiguration::class.memberProperties.filter { it is KMutableProperty<*> }.toMutableSet()
        val exclusions = listOf("classDiscriminatorMode", "exceptionsWithDebugInfo")
        for (e in exclusions) {
            assertTrue(mutableProperties.removeIf { it.name == e }, "Mutable property $e is in the exclusions list, but was not found among JsonConfiguration properties. Update the exclusions list.")
        }
        assertFalse(mutableProperties.isNotEmpty(), "Mutable properties found in JsonConfiguration: $mutableProperties")
    }
}