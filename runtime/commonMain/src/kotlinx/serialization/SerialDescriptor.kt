/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

/**
 * 
 */
public interface SerialDescriptor {

    /**
     *
     */
    public val serialName: String

    /**
     *
     */
    public val kind: SerialKind
    /**
     *
     */

    public val isNullable: Boolean get() = false

    /**
     *
     */
    public val elementsCount: Int get() = 0

    /**
     *
     */
    public fun getElementName(index: Int): String

    /**
     *
     */
    public fun getElementIndex(name: String): Int

    /**
     *
     */
    public fun getEntityAnnotations(): List<Annotation> = emptyList()

    /**
     *
     */
    public fun getElementAnnotations(index: Int): List<Annotation> = emptyList()

    /**
     *
     */
    public fun getElementDescriptor(index: Int): SerialDescriptor = TODO()

    /**
     *
     */
    public fun isElementOptional(index: Int): Boolean = false
}
