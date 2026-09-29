/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf.schema

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.test.Test
import kotlin.test.assertEquals

class InlinedClassesSchemaTest {
    @Serializable
    class SimpleClass

    @Serializable
    @JvmInline
    value class WrappedSimpleClass(val s: SimpleClass)

    @JvmInline
    @Serializable
    value class WrappedUInt(val i: UInt)


    @JvmInline
    @Serializable
    value class DeepWrappedUInt(val wrapped: WrappedUInt)

    @Serializable
    class DeepWrappedUIntHolder(val x: DeepWrappedUInt)

    @Serializable
    class SingleHolder(val uint: List<WrappedUInt>, val simple: List<WrappedSimpleClass>)

    @Serializable
    class MapHolder(val uint: Map<WrappedUInt, WrappedSimpleClass>, val simple: Map<WrappedSimpleClass, WrappedUInt>)


    @Serializable
    class NestedGenericMapHolder(val uint: List<Map<List<WrappedUInt>, WrappedSimpleClass>>, val simple: List<Map<List<WrappedSimpleClass>, WrappedUInt>>)

    @Serializable
    class ProtobufMapHolder(val uint: Map<WrappedUInt, WrappedSimpleClass>, val simple: Map<WrappedSimpleClass, WrappedUInt>, val long: Map<Long, WrappedUInt>)

    @Serializable
    class NestedGenericsHolder(val uint: List<List<WrappedUInt>>, val simple: List<List<WrappedSimpleClass>>)

    @Serializable
    class OptionalHolder(val uint: WrappedUInt? = null, val simple: WrappedSimpleClass? = null)

    @Test
    fun testInlined() {
        val generated = ProtoBufSchemaGenerator.generateSchemaText(listOf(
            SingleHolder.serializer().descriptor,
            NestedGenericsHolder.serializer().descriptor,
            MapHolder.serializer().descriptor,
            NestedGenericMapHolder.serializer().descriptor,
            ProtobufMapHolder.serializer().descriptor,
            OptionalHolder.serializer().descriptor,
            DeepWrappedUIntHolder.serializer().descriptor
        ))

        assertEquals(SCHEMA_TEXT, generated)
    }


    val SCHEMA_TEXT = """
        syntax = "proto2";


        // serial name 'kotlinx.serialization.protobuf.schema.InlinedClassesSchemaTest.SingleHolder'
        message SingleHolder {
          repeated int32 uint = 1;
          repeated SimpleClass simple = 2;
        }

        // serial name 'kotlinx.serialization.protobuf.schema.InlinedClassesSchemaTest.NestedGenericsHolder'
        message NestedGenericsHolder {
          repeated NestedGenericsHolder_uint uint = 1;
          repeated NestedGenericsHolder_simple simple = 2;
        }

        // serial name 'kotlinx.serialization.protobuf.schema.InlinedClassesSchemaTest.MapHolder'
        message MapHolder {
          map<int32, SimpleClass> uint = 1;
          repeated MapHolder_simple simple = 2;
        }

        // serial name 'kotlinx.serialization.protobuf.schema.InlinedClassesSchemaTest.NestedGenericMapHolder'
        message NestedGenericMapHolder {
          repeated NestedGenericMapHolder_uint uint = 1;
          repeated NestedGenericMapHolder_simple simple = 2;
        }

        // serial name 'kotlinx.serialization.protobuf.schema.InlinedClassesSchemaTest.ProtobufMapHolder'
        message ProtobufMapHolder {
          map<int32, SimpleClass> uint = 1;
          repeated ProtobufMapHolder_simple simple = 2;
          map<int64, int32> long = 3;
        }

        // serial name 'kotlinx.serialization.protobuf.schema.InlinedClassesSchemaTest.OptionalHolder'
        message OptionalHolder {
          // WARNING: a default value decoded when value is missing
          optional int32 uint = 1;
          // WARNING: a default value decoded when value is missing
          optional SimpleClass simple = 2;
        }

        // serial name 'kotlinx.serialization.protobuf.schema.InlinedClassesSchemaTest.DeepWrappedUIntHolder'
        message DeepWrappedUIntHolder {
          required int32 x = 1;
        }

        // serial name 'kotlinx.serialization.protobuf.schema.InlinedClassesSchemaTest.SimpleClass'
        message SimpleClass {
        }

        // This message was generated to support nested collection in list and does not present in Kotlin.
        // Containing message 'NestedGenericsHolder', field 'uint'
        message NestedGenericsHolder_uint {
          repeated int32 value = 1;
        }

        // This message was generated to support nested collection in list and does not present in Kotlin.
        // Containing message 'NestedGenericsHolder', field 'simple'
        message NestedGenericsHolder_simple {
          repeated SimpleClass value = 1;
        }

        // This message was generated to support legacy map and does not present in Kotlin.
        // Containing message 'MapHolder', field 'simple'
        message MapHolder_simple {
          required SimpleClass key = 1;
          required int32 value = 2;
        }

        // This message was generated to support nested collection in list and does not present in Kotlin.
        // Containing message 'NestedGenericMapHolder', field 'uint'
        message NestedGenericMapHolder_uint {
          repeated NestedGenericMapHolder_uint_value value = 1;
        }

        // This message was generated to support nested collection in list and does not present in Kotlin.
        // Containing message 'NestedGenericMapHolder', field 'simple'
        message NestedGenericMapHolder_simple {
          repeated NestedGenericMapHolder_simple_value value = 1;
        }

        // This message was generated to support legacy map and does not present in Kotlin.
        // Containing message 'ProtobufMapHolder', field 'simple'
        message ProtobufMapHolder_simple {
          required SimpleClass key = 1;
          required int32 value = 2;
        }

        // This message was generated to support legacy map and does not present in Kotlin.
        // Containing message 'NestedGenericMapHolder', field 'uint'
        message NestedGenericMapHolder_uint_value {
          repeated int32 key = 1;
          required SimpleClass value = 2;
        }

        // This message was generated to support legacy map and does not present in Kotlin.
        // Containing message 'NestedGenericMapHolder', field 'simple'
        message NestedGenericMapHolder_simple_value {
          repeated SimpleClass key = 1;
          required int32 value = 2;
        }

    """.trimIndent()
}