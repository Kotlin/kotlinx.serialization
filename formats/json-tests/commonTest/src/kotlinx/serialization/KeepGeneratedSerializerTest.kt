/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlin.jvm.*
import kotlin.test.*

class KeepGeneratedSerializerTest {
    @Serializable(with = ValueSerializer::class)
    @KeepGeneratedSerializer
    @JvmInline
    value class Value(val i: Int)

    object ValueSerializer: KSerializer<Value> {
        override val descriptor = PrimitiveSerialDescriptor("ValueSerializer", PrimitiveKind.INT)
        override fun deserialize(decoder: Decoder): Value {
            val value = decoder.decodeInt()
            return Value(value - 42)
        }
        override fun serialize(encoder: Encoder, value: Value) {
            encoder.encodeInt(value.i + 42)
        }
    }

    @Test
    fun testValueClass() {
        test(Value(1), "43", "1", Value.serializer(), Value.generatedSerializer())
    }



    @Serializable(with = DataSerializer::class)
    @KeepGeneratedSerializer
    data class Data(val i: Int)

    object DataSerializer: KSerializer<Data> {
        override val descriptor = PrimitiveSerialDescriptor("DataSerializer", PrimitiveKind.INT)
        override fun deserialize(decoder: Decoder): Data {
            val value = decoder.decodeInt()
            return Data(value)
        }
        override fun serialize(encoder: Encoder, value: Data) {
            encoder.encodeInt(value.i)
        }
    }

    @Test
    fun testDataClass() {
        test(Data(2), "2", "{\"i\":2}", Data.serializer(), Data.generatedSerializer())
    }


    @Serializable(with = ParentSerializer::class)
    @KeepGeneratedSerializer
    open class Parent(val p: Int) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Parent) return false

            if (p != other.p) return false

            return true
        }

        override fun hashCode(): Int {
            return p
        }
    }

    object ParentSerializer: KSerializer<Parent> {
        override val descriptor = PrimitiveSerialDescriptor("ParentSerializer", PrimitiveKind.INT)
        override fun deserialize(decoder: Decoder): Parent {
            val value = decoder.decodeInt()
            return Parent(value - 1)
        }
        override fun serialize(encoder: Encoder, value: Parent) {
            encoder.encodeInt(value.p + 1)
        }
    }

    @Serializable
    data class Child(val c: Int): Parent(0)

    @Serializable(with = ChildSerializer::class)
    @KeepGeneratedSerializer
    data class ChildWithCustom(val c: Int): Parent(0)

    object ChildSerializer: KSerializer<ChildWithCustom> {
        override val descriptor = PrimitiveSerialDescriptor("ChildSerializer", PrimitiveKind.INT)
        override fun deserialize(decoder: Decoder): ChildWithCustom {
            val value = decoder.decodeInt()
            return ChildWithCustom(value - 2)
        }

        override fun serialize(encoder: Encoder, value: ChildWithCustom) {
            encoder.encodeInt(value.c + 2)
        }
    }

    @Test
    fun testInheritance() {
        test(Parent(3), "4", "{\"p\":3}", Parent.serializer(), Parent.generatedSerializer())
        test(Child(4), "{\"p\":0,\"c\":4}", "", Child.serializer(), null)
        test(ChildWithCustom(5), "7", "{\"p\":0,\"c\":5}", ChildWithCustom.serializer(), ChildWithCustom.generatedSerializer())
    }


    @Serializable(with = MyEnumSerializer::class)
    @KeepGeneratedSerializer
    enum class MyEnum {
        A,
        B,
        FALLBACK
    }

    @Serializable
    data class EnumHolder(val e: MyEnum)

    object MyEnumSerializer: KSerializer<MyEnum> {
        val defaultSerializer = MyEnum.generatedSerializer()

        override val descriptor = PrimitiveSerialDescriptor("MyEnumSerializer", PrimitiveKind.INT)

        override fun deserialize(decoder: Decoder): MyEnum {
            decoder.decodeString()
            return MyEnum.A
        }

        override fun serialize(encoder: Encoder, value: MyEnum) {
            // always encode FALLBACK entry by generated serializer
            defaultSerializer.serialize(encoder, MyEnum.FALLBACK)
        }
    }

    @Test
    fun testEnum() {
        test(MyEnum.A, "\"FALLBACK\"", "\"A\"", MyEnum.serializer(), MyEnum.generatedSerializer())
        assertTrue(serializer<MyEnum>() is MyEnumSerializer, "serializer<MyEnum> illegal = " + serializer<MyEnum>())
        assertTrue(MyEnum.serializer() is MyEnumSerializer, "MyEnum.serializer() illegal = " + MyEnum.serializer())
        assertEquals("kotlinx.serialization.internal.EnumSerializer<kotlinx.serialization.KeepGeneratedSerializerTest.MyEnum>", MyEnum.generatedSerializer().toString(), "MyEnum.generatedSerializer() illegal")
        assertSame(MyEnum.generatedSerializer(), MyEnum.generatedSerializer(), "MyEnum.generatedSerializer() instance differs")
    }


    @Serializable(with = ParametrizedSerializer::class)
    @KeepGeneratedSerializer
    data class ParametrizedData<T>(val t: T)

    class ParametrizedSerializer(val serializer: KSerializer<Any>): KSerializer<ParametrizedData<Any>> {
        override val descriptor = PrimitiveSerialDescriptor("ParametrizedSerializer", PrimitiveKind.INT)

        override fun deserialize(decoder: Decoder): ParametrizedData<Any> {
            val value = serializer.deserialize(decoder)
            return ParametrizedData(value)
        }

        override fun serialize(encoder: Encoder, value: ParametrizedData<Any>) {
            serializer.serialize(encoder, value.t)
        }
    }

    @Test
    fun testParametrized() {
        test(
            ParametrizedData<Data>(Data(6)), "6", "{\"t\":6}", ParametrizedData.serializer(Data.serializer()), ParametrizedData.generatedSerializer(
                Data.serializer()))
    }


    @Serializable(WithCompanion.Companion::class)
    @KeepGeneratedSerializer
    data class WithCompanion(val value: Int) {
        @Serializer(WithCompanion::class)
        companion object {
            override val descriptor = PrimitiveSerialDescriptor("WithCompanionDesc", PrimitiveKind.INT)
            override fun deserialize(decoder: Decoder): WithCompanion {
                val value = decoder.decodeInt()
                return WithCompanion(value)
            }

            override fun serialize(encoder: Encoder, value: WithCompanion) {
                encoder.encodeInt(value.value)
            }
        }
    }

    @Test
    fun testCompanion() {
        test(WithCompanion(7), "7", "{\"value\":7}", WithCompanion.serializer(), WithCompanion.generatedSerializer())
    }


    @Serializable(with = ObjectSerializer::class)
    @KeepGeneratedSerializer
    object Object

    object ObjectSerializer: KSerializer<Object> {
        override val descriptor = PrimitiveSerialDescriptor("ObjectSerializer", PrimitiveKind.INT)

        override fun deserialize(decoder: Decoder): Object {
            decoder.decodeInt()
            return Object
        }
        override fun serialize(encoder: Encoder, value: Object) {
            encoder.encodeInt(8)
        }
    }

    @Test
    fun testObject() {
        test(Object, "8", "{}", Object.serializer(), Object.generatedSerializer())
        assertEquals("kotlinx.serialization.KeepGeneratedSerializerTest.Object()", Object.generatedSerializer().descriptor.toString(), "Object.generatedSerializer() illegal")
        assertSame(Object.generatedSerializer(), Object.generatedSerializer(), "Object.generatedSerializer() instance differs")
    }



    inline fun <reified T : Any> test(
        value: T,
        customJson: String,
        keepJson: String,
        serializer: KSerializer<T>,
        generatedSerializer: KSerializer<T>?
    ) {
        val implicitJson = Json.encodeToString(value)
        assertEquals(customJson, implicitJson, "Json.encodeToString(value: ${T::class.simpleName})")
        val implicitDecoded = Json.decodeFromString<T>(implicitJson)
        assertEquals(value, implicitDecoded, "Json.decodeFromString(json): ${T::class.simpleName}")

        val exlicitJson = Json.encodeToString(serializer, value)
        assertEquals(customJson, exlicitJson, "Json.encodeToString(${T::class.simpleName}.serializer(), value)")
        val explicitDecoded = Json.decodeFromString(serializer, exlicitJson)
        assertEquals(value, explicitDecoded, "Json.decodeFromString(${T::class.simpleName}.serializer(), json)")

        if (generatedSerializer == null) return
        val keep = Json.encodeToString(generatedSerializer, value)
        assertEquals(keepJson, keep, "Json.encodeToString(${T::class.simpleName}.generatedSerializer(), value)")
        val keepDecoded = Json.decodeFromString(generatedSerializer, keep)
        assertEquals(value, keepDecoded, "Json.decodeFromString(${T::class.simpleName}.generatedSerializer(), json)")
    }

}