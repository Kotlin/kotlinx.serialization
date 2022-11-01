/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.json.Json
import java.net.URLClassLoader
import kotlin.reflect.*
import kotlin.test.*

class SerializerByTypeCacheTest {

    @Serializable
    class Holder(val i: Int)

    @Suppress("UNCHECKED_CAST")
    @Test
    fun testCaching() {
        val typeOfKType = typeOf<Holder>()
        val parameterKType = typeOf<List<Holder>>().arguments[0].type!!
        assertSame(serializer(), serializer<Holder>())
        assertSame(serializer(typeOfKType), serializer(typeOfKType))
        assertSame(serializer(parameterKType), serializer(parameterKType))
        assertSame(serializer(), serializer(typeOfKType) as KSerializer<Holder>)
        assertSame(serializer(parameterKType) as KSerializer<Holder>, serializer(typeOfKType) as KSerializer<Holder>)
    }

    /**
     * Checking the case when a parameterized type is loaded in different parallel [ClassLoader]s.
     *
     * If the main type is loaded by a common parent [ClassLoader] (for example, a bootstrap for [List]),
     * and the element class is loaded by different loaders, then some implementations of the [KType] (e.g. `KTypeImpl` from reflection) may not see the difference between them.
     *
     * As a result, a serializer for another loader will be returned from the cache, and it will generate instances, when working with which we will get an [ClassCastException].
     *
     * The test checks the correctness of the cache for such cases - that different serializers for different loaders will be returned.
     *
     * [see](https://youtrack.jetbrains.com/issue/KT-54523).
     */
    @Test
    fun testDifferentClassLoaders() {
        val elementKType1 = SimpleKType(loadClass().kotlin)
        val elementKType2 = SimpleKType(loadClass().kotlin)

        // Java class must be same (same name)
        assertEquals(elementKType1.classifier.java.canonicalName, elementKType2.classifier.java.canonicalName)
        // class loaders must be different
        assertNotSame(elementKType1.classifier.java.classLoader, elementKType2.classifier.java.classLoader)
        // due to the incorrect definition of the `equals`, KType-s are equal
        assertEquals(elementKType1, elementKType2)

        // create parametrized type `List<Foo>`
        val kType1 = SingleParametrizedKType(List::class, elementKType1)
        val kType2 = SingleParametrizedKType(List::class, elementKType2)

        val serializer1 = serializer(kType1)
        val serializer2 = serializer(kType2)

        // when taking a serializers from cache, we must distinguish between KType-s, despite the fact that they are equivalent
        assertNotSame(serializer1, serializer2)

        // serializers must work correctly
        Json.decodeFromString(serializer1, "[{\"i\":1}]")
        Json.decodeFromString(serializer2, "[{\"i\":1}]")
    }

    /**
     * Load class `example.Foo` via new class loader. Compiled class-file located in the resources.
     */
    private fun loadClass(): Class<*> {
        val classesUrl = this::class.java.classLoader.getResource("class_loaders/classes/")
        val loader1 = URLClassLoader(arrayOf(classesUrl), this::class.java.classLoader)
        return loader1.loadClass("example.Foo")
    }

    private class SimpleKType(override val classifier: KClass<*>): KType {
        override val annotations: List<Annotation> = emptyList()
        override val arguments: List<KTypeProjection> = emptyList()

        override val isMarkedNullable: Boolean = false

        override fun equals(other: Any?): Boolean {
            if (other !is SimpleKType) return false
            return classifier.java.canonicalName == other.classifier.java.canonicalName
        }

        override fun hashCode(): Int {
            return classifier.java.canonicalName.hashCode()
        }
    }


    private class SingleParametrizedKType(override val classifier: KClass<*>, val parameterType: KType): KType {
        override val annotations: List<Annotation> = emptyList()

        override val arguments: List<KTypeProjection> = listOf(KTypeProjection(KVariance.INVARIANT, parameterType))

        override val isMarkedNullable: Boolean = false

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SingleParametrizedKType

            if (classifier != other.classifier) return false
            if (parameterType != other.parameterType) return false

            return true
        }

        override fun hashCode(): Int {
            var result = classifier.hashCode()
            result = 31 * result + parameterType.hashCode()
            return result
        }
    }
}
