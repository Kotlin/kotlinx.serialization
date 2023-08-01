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

internal inline fun <reified T: Throwable> failBefore(kotlinVersion: String, test: () -> Unit) {
    val boundVersion = kotlinVersion.toKotlinVersion()

    var error: Throwable? = null
    try {
        test()
    } catch (e: Throwable) {
        error = e
    }

    if (currentKotlinVersion < boundVersion) {
        if (error == null) {
            throw Exception("Exception with type '${T::class.simpleName}' expected for version '$currentKotlinVersion' < '$boundVersion'")
        }
        if (error !is T) throw Exception("Illegal exception type, expected '${T::class.simpleName}' actual '${error::class.simpleName}' in version '$currentKotlinVersion' < '$boundVersion'", error)
    } else {
        if (error != null) throw Exception("Unexpected error in version '$currentKotlinVersion' >= '$boundVersion'", error)
    }
}

internal inline fun <reified T: Throwable> failSince(kotlinVersion: String, test: () -> Unit) {
    val boundVersion = kotlinVersion.toKotlinVersion()

    var error: Throwable? = null
    try {
        test()
    } catch (e: Throwable) {
        error = e
    }

    if (currentKotlinVersion >= boundVersion) {
        if (error == null) {
            throw Exception("Exception with type '${T::class.simpleName}' expected for version '$currentKotlinVersion' >= '$boundVersion'")
        }
        if (error !is T) throw Exception("Illegal exception type, expected '${T::class.simpleName}' actual '${error::class.simpleName}' in version '$currentKotlinVersion' >= '$boundVersion'", error)
    } else {
        if (error != null) throw Exception("Unexpected error in version '$currentKotlinVersion' < '$boundVersion'", error)
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
        failBefore<IllegalArgumentException>("0.0.0") {
            // no-op
        }

        // error if there is no exception and if current version is before of the specified
        assertFails {
            failBefore<IllegalArgumentException>("255.255.255") {
                // no-op
            }
        }

        // ok if thrown expected exception if current version is before of the specified
        failBefore<IllegalArgumentException>("255.255.255") {
            throw IllegalArgumentException()
        }

        // ok if thrown unexpected exception if current version is before of the specified
        assertFails {
            failBefore<IllegalArgumentException>("255.255.255") {
                throw Exception()
            }
        }
    }

    @Test
    fun testFailSince() {
        // ok if there is no exception if current version less then specified
        failSince<IllegalArgumentException>("255.255.255") {
            // no-op
        }

        // error if there is no exception and if current version is greater or equals specified
        assertFails {
            failSince<IllegalArgumentException>("0.0.0") {
                // no-op
            }
        }

        // ok if thrown expected exception if current version is greater or equals specified
        failSince<IllegalArgumentException>("0.0.0") {
            throw IllegalArgumentException()
        }

        // ok if thrown unexpected exception if current version is greater or equals specified
        assertFails {
            failSince<IllegalArgumentException>("0.0.0") {
                throw Exception()
            }
        }
    }

}
