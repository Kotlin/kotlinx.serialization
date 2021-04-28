/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.polymorphic

import kotlinx.serialization.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlinx.serialization.json.internal.JsonLexer
import kotlinx.serialization.json.internal.JsonTreeReader
import kotlin.test.*

class JsonContentPolymorphicSerializerTest : JsonTestBase() {
    val json = Json

    @Serializable
    sealed class Choices {
        @Serializable
        data class HasA(val a: String) : Choices()

        @Serializable
        data class HasB(val b: Int) : Choices()

        @Serializable
        data class HasC(val c: Boolean) : Choices()
    }

    object ChoicesParametricSerializer : JsonContentPolymorphicSerializer<Choices>(Choices::class) {
        override fun selectDeserializer(element: JsonElement, elementName: String): KSerializer<out Choices> {
            val obj = element.jsonObject
            return when {
                "a" in obj -> Choices.HasA.serializer()
                "b" in obj -> Choices.HasB.serializer()
                "c" in obj -> Choices.HasC.serializer()
                else -> throw SerializationException("Unknown choice")
            }
        }
    }

    @Serializable
    data class WithChoices(@Serializable(ChoicesParametricSerializer::class) val response: Choices)

    private val testDataInput = listOf(
        """{"response":{"a":"string"}}""",
        """{"response":{"b":42}}""",
        """{"response":{"c":true}}"""
    )

    private val testDataOutput = listOf(
        WithChoices(Choices.HasA("string")),
        WithChoices(Choices.HasB(42)),
        WithChoices(Choices.HasC(true))
    )

    @Test
    fun testParsesParametrically() = parametrizedTest { streaming ->
        for (i in testDataInput.indices) {
            assertEquals(
                testDataOutput[i],
                json.decodeFromString(WithChoices.serializer(), testDataInput[i], streaming),
                "failed test on ${testDataInput[i]}, useStreaming = $streaming"
            )
        }
    }

    @Test
    fun testSerializesParametrically() = parametrizedTest { streaming ->
        for (i in testDataOutput.indices) {
            assertEquals(
                testDataInput[i],
                json.encodeToString(WithChoices.serializer(), testDataOutput[i], streaming),
                "failed test on ${testDataOutput[i]}, useStreaming = $streaming"
            )
        }
    }

    interface Payment {
        val amount: String
    }

    @Serializable
    data class SuccessfulPayment(override val amount: String, val date: String) : Payment

    @Serializable
    data class RefundedPayment(override val amount: String, val date: String, val reason: String) : Payment

    object PaymentSerializer : JsonContentPolymorphicSerializer<Payment>(Payment::class) {
        override fun selectDeserializer(element: JsonElement, elementName: String) = when {
            "reason" in element.jsonObject -> RefundedPayment.serializer()
            else -> SuccessfulPayment.serializer()
        }
    }

    @Test
    fun testDocumentationSample() = parametrizedTest { streaming ->
        assertEquals(
            SuccessfulPayment("1.0", "03.02.2020"),
            json.decodeFromString(PaymentSerializer, """{"amount":"1.0","date":"03.02.2020"}""", streaming)
        )
        assertEquals(
            RefundedPayment("2.0", "03.02.2020", "complaint"),
            json.decodeFromString(PaymentSerializer, """{"amount":"2.0","date":"03.02.2020","reason":"complaint"}""", streaming)
        )
    }

    @Serializable
    data class Person(
        val name: String,
        @JsonNames("deceasedBoolean","deceasedDate")
        val deceased: MultiType
    )

    @Serializable(with = MultiTypeSerializer::class)
    abstract class MultiType()

    @Serializable(with = DateTypeSerializer::class)
    data class DateType(val value: String) : MultiType()

    @Serializable(with = BooleanTypeSerializer::class)
    data class BooleanType(val value: Boolean) : MultiType()

    @Serializer(forClass = DateType::class)
    object DateTypeSerializer {
        override fun deserialize(decoder: Decoder): DateType {
            val value = decoder.decodeString()
            return DateType(value)
        }

        override fun serialize(encoder: Encoder, value: DateType) {
            encoder.encodeString(value.value)
        }
    }

    @Serializer(forClass = BooleanType::class)
    object BooleanTypeSerializer {
        override fun deserialize(decoder: Decoder): BooleanType {
            val value = decoder.decodeBoolean()
            return BooleanType(value)
        }

        override fun serialize(encoder: Encoder, value: BooleanType) {
            encoder.encodeBoolean(value.value)
        }
    }

    @Serializer(forClass = MultiType::class)
    object MultiTypeSerializer : JsonContentPolymorphicSerializer<MultiType>(MultiType::class) {
        override fun selectDeserializer(element: JsonElement, elementName: String) =
            when (elementName) {
                "deceasedBoolean" -> BooleanTypeSerializer
                "deceasedDate" -> DateTypeSerializer
                else -> throw NotImplementedError("Serializer for property '$elementName' has not been implemented.")
            }
    }

    @Test
    fun testPersonSample() = parametrizedTest { streaming ->
        assertEquals(
            Person("MyName1", BooleanType(true)),
            json.decodeFromString(
                Person.serializer(),
                """{"name":"MyName1","deceasedBoolean":true}""",
                streaming
            )
        )
        assertEquals(
            Person("MyName2", DateType("1990-10-23")),
            json.decodeFromString(
                Person.serializer(),
                """{"name":"MyName2","deceasedDate":"1990-10-23"}""",
                streaming
            )
        )
    }
}
