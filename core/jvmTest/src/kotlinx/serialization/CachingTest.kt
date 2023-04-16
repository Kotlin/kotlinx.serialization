/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.internal.createCache
import kotlinx.serialization.internal.createParametrizedCache
import kotlinx.serialization.internal.kclass
import kotlinx.serialization.modules.*
import org.junit.Test
import kotlin.reflect.KClass
import kotlin.reflect.typeOf
import kotlin.test.*

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
            clazz.parametrizedSerializerOrNull(types, serializers)
        }

        repeat(10) {
            cache.get(typeOf<Map<*, *>>().kclass(), listOf(typeOf<String>(), typeOf<String>()))
        }

        assertEquals(1, factoryCalled)
    }
}
