/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.protobuf.internal.ProtobufDecodingException
import kotlin.test.Ignore
import kotlin.test.Test

/**
 * Tests for [assertFailsWith] to see if output in IDEA can be checked with <Click to see difference> button.
 * Expected to fail so ignore in CI.
 */
@Ignore
class TestFunctionTest {
    @Test
    fun testAssertionMessage() {
        assertFailsWith<IllegalArgumentException>(assertion = {
            assertFailsWith("expected message")
        }) {
            throw IllegalArgumentException("actual message")
        }
    }
    @Test
    fun testAssertionType() {
        assertFailsWith<IllegalArgumentException>(assertion = {
            assertFailsWith("")
            assertCausedBy<ProtobufDecodingException> {
                assertFailsWith("expected message")
            }
        }) {
            throw IllegalArgumentException("", IllegalArgumentException())
        }
    }
    @Test
    fun testAssertionFailWith() {
        assertFailsWith<NumberFormatException>(assertion = {}) {
            throw ProtobufDecodingException("expected message")
        }
    }
}