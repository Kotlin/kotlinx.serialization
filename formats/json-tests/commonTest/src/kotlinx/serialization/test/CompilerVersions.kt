/*
 * Copyright 2017-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.test

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val currentKotlinVersion = KotlinVersion.CURRENT

private fun String.toKotlinVersion(): KotlinVersion {
    val parts = split(".")
    val intParts = parts.mapNotNull { it.toIntOrNull() }
    if (parts.size != 3 || intParts.size != 3) error("Illegal kotlin version, expected format is 1.2.3")

    return KotlinVersion(intParts[0], intParts[1], intParts[2])
}

internal fun runSince(kotlinVersion: String, test: () -> Unit) {
    if (currentKotlinVersion >= kotlinVersion.toKotlinVersion()) {
        test()
    }
}

internal class CompilerVersionTest {
    @Test
    fun testSince() {
        var executed = false

        runSince("1.0.0") {
            executed = true
        }
        assertTrue(executed)

        executed = false
        runSince("255.255.255") {
            executed = true
        }
        assertFalse(executed)
    }
}
