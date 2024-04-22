/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*
import kotlin.reflect.*
import kotlin.test.*
import kotlin.test.Test

class CachingTest {
    @Test
    fun testCache() {
        var factoryCalled = 0

        val cache = createCache {
            factoryCalled += 1
            it.serializerOrNull()
        }

        repeat(10) {
            cache.get(typeOf<String>().kclass())
        }

        assertEquals(1, factoryCalled)
    }

    @Test
    fun testParameterizedCache() {
        var factoryCalled = 0

        val cache = createParametrizedCache { clazz, types ->
            factoryCalled += 1
            val serializers = EmptySerializersModule().serializersForParameters(types, true)!!
            clazz.parametrizedSerializerOrNull(serializers) { types[0].classifier }
        }

        repeat(10) {
            cache.get(typeOf<Map<*, *>>().kclass(), listOf(typeOf<String>(), typeOf<String>()))
        }

        assertEquals(1, factoryCalled)
    }

    @Serializable
    class Target

    @Test
    fun testJvmIntrinsics() {
        val ser1 = Target.serializer()
        assertFalse(SERIALIZERS_CACHE.isStored(Target::class), "Cache shouldn't have values before call to serializer<T>()")
        val ser2 = serializer<Target>()
        assertFalse(
            SERIALIZERS_CACHE.isStored(Target::class),
            "Serializer for Target::class is stored in the cache, which means that runtime lookup was performed and call to serializer<Target> was not intrinsified." +
                "Check that compiler plugin intrinsics are enabled and working correctly."
        )
        val ser3 = serializer(typeOf<Target>())
        assertTrue(SERIALIZERS_CACHE.isStored(Target::class), "Serializer should be stored in cache after typeOf-based lookup")
    }

    @Serializable
    class Target2

    inline fun <reified T : Any> indirect(): KSerializer<T> = serializer<T>()

    @Test
    fun testJvmIntrinsicsIndirect() {
        val ser1 = Target2.serializer()
        assertFalse(SERIALIZERS_CACHE.isStored(Target2::class), "Cache shouldn't have values before call to serializer<T>()")
        val ser2 = indirect<Target2>()
        assertFalse(
            SERIALIZERS_CACHE.isStored(Target2::class),
            "Serializer for Target2::class is stored in the cache, which means that runtime lookup was performed and call to serializer<Target2> was not intrinsified." +
                "Check that compiler plugin intrinsics are enabled and working correctly."
        )
    }
}
