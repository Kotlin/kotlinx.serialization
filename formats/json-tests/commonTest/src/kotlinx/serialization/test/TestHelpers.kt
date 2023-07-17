/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package kotlinx.serialization.test

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.internal.ESCAPE_STRINGS
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.test.*

fun SerialDescriptor.assertDescriptorEqualsTo(other: SerialDescriptor) {
    assertEquals(serialName, other.serialName)
    assertEquals(elementsCount, other.elementsCount)
    assertEquals(isNullable, other.isNullable)
    assertEquals(annotations, other.annotations)
    assertEquals(kind, other.kind)
    for (i in 0 until elementsCount) {
        getElementDescriptor(i).assertDescriptorEqualsTo(other.getElementDescriptor(i))
        val name = getElementName(i)
        val otherName = other.getElementName(i)
        assertEquals(name, otherName)
        assertEquals(getElementAnnotations(i), other.getElementAnnotations(i))
        assertEquals(name, otherName)
        assertEquals(isElementOptional(i), other.isElementOptional(i))
    }
}

inline fun noJs(test: () -> Unit) {
    if (!isJs()) test()
}

inline fun jvmOnly(test: () -> Unit) {
    if (isJvm()) test()
}

inline fun assertFailsWithMissingField(block: () -> Unit) {
    val e = assertFailsWith<SerializationException>(block = block)
    assertTrue(e.message?.contains("but it was missing") ?: false)
}

fun generateRandomUnicodeString(size: Int): String {
    return buildString(size) {
        repeat(size) {
            val pickEscape = Random.nextBoolean()
            if (pickEscape) {
                // Definitely an escape symbol
                append(ESCAPE_STRINGS.random().takeIf { it != null } ?: 'N')
            } else {
                // Any symbol, including escaping one
                append(Char(Random.nextInt(Char.MIN_VALUE.code..Char.MAX_VALUE.code)).takeIf { it.isDefined() && !it.isSurrogate()} ?: 'U')
            }
        }
    }
}
