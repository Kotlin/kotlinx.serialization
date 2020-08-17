/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("DEPRECATION_ERROR")
package kotlinx.serialization.test

import kotlinx.serialization.descriptors.*
import kotlinx.serialization.internal.EnumSerializer
import kotlin.test.*

@Suppress("TestFunctionName")
internal inline fun <reified E : Enum<E>> EnumSerializer(serialName: String): EnumSerializer<E> =
    EnumSerializer(serialName, enumValues())


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

