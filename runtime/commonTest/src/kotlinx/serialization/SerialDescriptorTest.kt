/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.internal.*
import kotlin.test.*

class SerialDescriptorTest {

    @Serializable
    class Foo(val a: String)


    @Serializable
    class A(val x: Int?, val z: String = "")

    @Test
    fun foo() {
        val d = A.serializer().descriptor
        println(d.isElementOptional(0))
        println(d.isElementOptional(1))
    }

    @Test
    fun testDescriptorContract() {
        val d = Foo.serializer().descriptor
        assertEquals(StructureKind.CLASS, d.kind)
        assertEquals("kotlinx.serialization.SerialDescriptorTest.Foo", d.serialName)
        assertEquals(0, d.getElementIndex("a"))
        assertEquals(0, d.getElementAnnotations(0).size)
        assertEquals("a", d.getElementName(0))
        assertEquals(StringDescriptor, d.getElementDescriptor(0))

        println(kotlin.runCatching { d.getElementIndex("FFF") })
        println(kotlin.runCatching { d.getElementAnnotations(42) })
        println(kotlin.runCatching { d.getElementName(3) })
        println(kotlin.runCatching { d.getElementDescriptor(3) })
        println(kotlin.runCatching { d.isElementOptional(3) })
        println(kotlin.runCatching { d.elementsCount })
    }
}