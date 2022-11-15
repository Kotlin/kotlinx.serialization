/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.modules.*
import kotlinx.serialization.test.noJsLegacy
import kotlin.test.*
import kotlin.time.*

class CachedSerializersTest {
    @Serializable
    object Object

    @Serializable
    sealed class SealedParent {
        @Serializable
        data class Child(val i: Int) : SealedParent()
    }

    @Serializable
    abstract class Abstract

    @Serializable
    enum class SerializableEnum {A, B}

    @SerialInfo
    annotation class MyAnnotation(val x: Int)

    @Serializable
    enum class SerializableMarkedEnum {
        @SerialName("first")
        @MyAnnotation(1)
        C,
        @MyAnnotation(2)
        D
    }

    @Test
    fun testObjectSerializersAreSame() = noJsLegacy {
        assertSame(Object.serializer(), Object.serializer())
    }

    @Test
    fun testSerializableEnumSerializersAreSame() = noJsLegacy {
        assertSame(SerializableEnum.serializer(), SerializableEnum.serializer())
    }

    @Test
    fun testSerializableMarkedEnumSerializersAreSame() = noJsLegacy {
        assertSame(SerializableMarkedEnum.serializer(), SerializableMarkedEnum.serializer())
    }

    @Test
    fun testSealedSerializersAreSame() = noJsLegacy {
        assertSame(SealedParent.serializer(), SealedParent.serializer())
    }

    @Test
    fun testAbstractSerializersAreSame() = noJsLegacy {
        assertSame(Abstract.serializer(), Abstract.serializer())
    }


    @OptIn(ExperimentalTime::class)
    @Test
    @Ignore // TODO: Unignore after 1.8.0 update
    fun testSerializersAreIntrinsified() {
        val m = SerializersModule {  }
        val direct = measureTime {
            Object.serializer()
        }
        val directMs = direct.inWholeMicroseconds
        val indirect = measureTime {
            m.serializer<Object>()
        }
        val indirectMs = indirect.inWholeMicroseconds
        if (indirectMs > directMs + (directMs / 4)) error("Direct ($directMs) and indirect ($indirectMs) times are too far apart")
    }
}

