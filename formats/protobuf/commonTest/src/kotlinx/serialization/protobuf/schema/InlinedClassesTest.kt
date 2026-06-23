/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf.schema

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.test.Test
import kotlin.test.assertEquals

class InlinedClassesTest {
    @Serializable
    class SimpleClass

    @Serializable
    @JvmInline
    value class WrappedSimpleClass(val s: SimpleClass)

    @JvmInline
    @Serializable
    value class WrappedUInt(val i: UInt)

    @Serializable
    class SingleHolder(val unit: List<WrappedUInt>, val simple: List<WrappedSimpleClass>)

    @Serializable
    class DoubleHolder(val unit: List<List<WrappedUInt>>, val simple: List<List<WrappedSimpleClass>>)


    @Test
    fun testInlined() {
        val generated = ProtoBufSchemaGenerator.generateSchemaText(listOf(
            SingleHolder.serializer().descriptor,
            DoubleHolder.serializer().descriptor,
        ))

        assertEquals(SCHEMA_TEXT, generated)
    }


    val SCHEMA_TEXT = """
        syntax = "proto2";


        // serial name 'kotlinx.serialization.protobuf.schema.InlinedClassesTest.SingleHolder'
        message SingleHolder {
          repeated int32 unit = 1;
          repeated SimpleClass simple = 2;
        }

        // serial name 'kotlinx.serialization.protobuf.schema.InlinedClassesTest.DoubleHolder'
        message DoubleHolder {
          repeated DoubleHolder_unit unit = 1;
          repeated DoubleHolder_simple simple = 2;
        }

        // serial name 'kotlinx.serialization.protobuf.schema.InlinedClassesTest.SimpleClass'
        message SimpleClass {
        }

        // This message was generated to support nested collection in list and does not present in Kotlin.
        // Containing message 'DoubleHolder', field 'unit'
        message DoubleHolder_unit {
          repeated int32 value = 1;
        }

        // This message was generated to support nested collection in list and does not present in Kotlin.
        // Containing message 'DoubleHolder', field 'simple'
        message DoubleHolder_simple {
          repeated SimpleClass value = 1;
        }

    """.trimIndent()
}