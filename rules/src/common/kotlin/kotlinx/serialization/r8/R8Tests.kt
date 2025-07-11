/*
 * Copyright 2017-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.r8

import kotlinx.serialization.*
import java.lang.annotation.Annotation
import kotlin.reflect.*
import kotlin.test.*

class R8Tests {
    val checker: R8Checker = getR8Checker()

    /**
     * Test classes renames and deleted as long as methods.
     */
    @Test
    fun testOptimisation() {
        val unusedClass = checker.findClass("kotlinx.serialization.r8.UnusedClass")
        assertTrue(unusedClass.isShrunk)

        ObfuscatedClass("World").used()
        val obfuscated = checker.findClass(ObfuscatedClass::class)
        assertTrue(obfuscated.isObfuscated)

        val used = obfuscated.findMethod("used")
        assertTrue(used.isObfuscated)
        assertFalse(used.isShrunk)

        val unused = obfuscated.findMethod("unused")
        assertTrue(unused.isShrunk)
    }

    @Test
    fun testCompanions() {
        assertSerializerWithCompanion<SerializableSimple>()
        assertSerializerWithCompanion<Container.SerializableNested>()
        assertSerializerWithCompanion<SerializableEnum>()

        assertSerializerWithCompanion<SealedInterface>()
        assertSerializerWithCompanion<AbstractClass>()
        assertSerializerWithCompanion<OpenPolymorphicClass>()
        assertSerializerWithCompanion<OpenClass>()
    }

    @Test
    fun testNamedCompanions() {
        assertSerializerWithNamedCompanion<SerializableWithNamedCompanion>("CustomName")
    }

    @Test
    fun testSerializableObject() {
        assertSerializerForObject<SerializableObject>()
    }

    /**
     * Test serialization annotations are saved and aren't renamed.
     */
    @Test
    fun testAnnotations() {
        assertTrue(SerializableSimple::class.java.hasAnnotation("kotlinx.serialization.Serializable"))
        assertTrue(OpenPolymorphicClass::class.java.hasAnnotation("kotlinx.serialization.Serializable"))
        assertTrue(OpenPolymorphicClass::class.java.hasAnnotation("kotlinx.serialization.Polymorphic"))
    }

    /**
     * Descriptor field should present.
     * Using reflection here because the private field `descriptor` isn't present in the mapping file.
     */
    @Test
    fun testDescriptorField() {
        assertTrue(AccessSerializer.serializer()::class.java.declaredFields.any { it.name == "descriptor" })
    }



    private inline fun <reified T> assertSerializerWithCompanion() {
        val companionName = "Companion"

        val serializable = checker.findClass(T::class)
        val field = serializable.findField(companionName)

        assertFalse(field.isObfuscated, "Companion field '${serializable.originalName}#$companionName' should not be obfuscated")
        assertFalse(field.isShrunk, "Companion field '${serializable.originalName}#$companionName' should not be shrunk")

        val companion = checker.findClass(serializable.originalName + "$" + companionName)
        val serializer = companion.findMethod("serializer")
        assertFalse(serializer.isShrunk)
        assertFalse(serializer.isObfuscated)

        serializer(typeOf<T>())
    }

    private inline fun <reified T> assertSerializerWithNamedCompanion(companionName: String) {
        // somewhy R8 doesn't print field for named companion in mapping.txt, so we check it by reflection
        T::class.java.getDeclaredField(companionName)
        serializer(typeOf<T>())
    }

    private inline fun <reified T> assertSerializerForObject() {
        val serializable = checker.findClass(T::class)
        val field = serializable.findField("INSTANCE")

        assertFalse(field.isObfuscated, "Field 'INSTANCE' should not be obfuscated")
        assertFalse(field.isShrunk, "Field 'INSTANCE' should not be shrunk")

        val serializer = serializable.findMethod("serializer")
        assertFalse(serializer.isObfuscated, "Method 'serializer()' should not be obfuscated")
        assertFalse(serializer.isShrunk, "Method 'serializer()' should not be shrunk")

        serializer(typeOf<T>())
    }

    fun Class<*>.hasAnnotation(annotationName: String): Boolean {
        return annotations.any { (it as Annotation).annotationType().name == annotationName }
    }

}
