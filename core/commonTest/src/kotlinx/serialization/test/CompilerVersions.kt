/*
 * Copyright 2017-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.test

import kotlin.test.*

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


internal inline fun <reified T : Throwable> shouldFail(
    sinceKotlin: String? = null,
    beforeKotlin: String? = null,
    onJvm: Boolean = true,
    onJs: Boolean = true,
    onNative: Boolean = true,
    onWasm: Boolean = true,
    test: () -> Unit
) {
    val args = mapOf(
        "since" to sinceKotlin,
        "before" to beforeKotlin,
        "onJvm" to onJvm,
        "onJs" to onJs,
        "onNative" to onNative,
        "onWasm" to onWasm
    )

    val sinceVersion = sinceKotlin?.toKotlinVersion()
    val beforeVersion = beforeKotlin?.toKotlinVersion()

    val version = (sinceVersion != null && currentKotlinVersion >= sinceVersion)
        || (beforeVersion != null && currentKotlinVersion < beforeVersion)

    val platform = (isJvm() && onJvm) || (isJs() && onJs) || (isNative() && onNative) || (isWasm() && onWasm)

    var error: Throwable? = null
    try {
        test()
    } catch (e: Throwable) {
        error = e
    }

    if (version && platform) {
        if (error == null) {
            throw AssertionError("Exception with type '${T::class.simpleName}' expected for $args")
        }
        if (error !is T) throw AssertionError(
            "Illegal exception type, expected '${T::class.simpleName}' actual '${error::class.simpleName}' for $args",
            error
        )
    } else {
        if (error != null) throw AssertionError(
            "Unexpected error for $args",
            error
        )
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

    @Test
    fun testFailBefore() {
        // ok if there is no exception if current version greater is before of the specified
        shouldFail<IllegalArgumentException>(beforeKotlin = "0.0.0") {
            // no-op
        }

        // error if there is no exception and if current version is before of the specified
        assertFails {
            shouldFail<IllegalArgumentException>(beforeKotlin = "255.255.255") {
                // no-op
            }
        }

        // ok if thrown expected exception if current version is before of the specified
        shouldFail<IllegalArgumentException>(beforeKotlin = "255.255.255") {
            throw IllegalArgumentException()
        }

        // ok if thrown unexpected exception if current version is before of the specified
        assertFails {
            shouldFail<IllegalArgumentException>(beforeKotlin = "255.255.255") {
                throw Exception()
            }
        }

    }

    @Test
    fun testFailSince() {
        // ok if there is no exception if current version less then specified
        shouldFail<IllegalArgumentException>(sinceKotlin = "255.255.255") {
            // no-op
        }

        // error if there is no exception and if current version is greater or equals specified
        assertFails {
            shouldFail<IllegalArgumentException>(sinceKotlin = "0.0.0") {
                // no-op
            }
        }

        // ok if thrown expected exception if current version is greater or equals specified
        shouldFail<IllegalArgumentException>(sinceKotlin = "0.0.0") {
            throw IllegalArgumentException()
        }

        // ok if thrown unexpected exception if current version is greater or equals specified
        assertFails {
            shouldFail<IllegalArgumentException>(sinceKotlin = "0.0.0") {
                throw Exception()
            }
        }
    }

    @Test
    fun testExcludePlatform() {
        if (isJvm()) {
            shouldFail<IllegalArgumentException>(beforeKotlin = "255.255.255", onJvm = false) {
                // no-op
            }
            shouldFail<IllegalArgumentException>(sinceKotlin = "0.0.0", onJvm = false) {
                // no-op
            }
            shouldFail<IllegalArgumentException>(sinceKotlin = "0.0.0", beforeKotlin = "255.255.255", onJvm = false) {
                // no-op
            }
        } else if (isJs()) {
            shouldFail<IllegalArgumentException>(beforeKotlin = "255.255.255", onJs = false) {
                // no-op
            }
            shouldFail<IllegalArgumentException>(sinceKotlin = "0.0.0", onJs = false) {
                // no-op
            }
            shouldFail<IllegalArgumentException>(sinceKotlin = "0.0.0", beforeKotlin = "255.255.255", onJs = false) {
                // no-op
            }
        } else if (isWasm()) {
            shouldFail<IllegalArgumentException>(beforeKotlin = "255.255.255", onWasm = false) {
                // no-op
            }
            shouldFail<IllegalArgumentException>(sinceKotlin = "0.0.0", onWasm = false) {
                // no-op
            }
            shouldFail<IllegalArgumentException>(sinceKotlin = "0.0.0", beforeKotlin = "255.255.255", onWasm = false) {
                // no-op
            }
        } else if (isNative()) {
            shouldFail<IllegalArgumentException>(beforeKotlin = "255.255.255", onNative = false) {
                // no-op
            }
            shouldFail<IllegalArgumentException>(sinceKotlin = "0.0.0", onNative = false) {
                // no-op
            }
            shouldFail<IllegalArgumentException>(sinceKotlin = "0.0.0", beforeKotlin = "255.255.255", onNative = false) {
                // no-op
            }
        }
    }

}
