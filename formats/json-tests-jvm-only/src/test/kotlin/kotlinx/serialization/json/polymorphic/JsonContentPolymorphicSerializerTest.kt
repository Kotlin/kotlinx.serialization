/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.polymorphic

import kotlinx.serialization.*
import kotlinx.serialization.json.*
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
        override fun selectDeserializer(element: JsonElement): KSerializer<out Choices> {
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
                "failed test on ${testDataInput[i]}, jsonTestingMode = $streaming"
            )
        }
    }

    @Test
    fun testSerializesParametrically() = parametrizedTest { streaming ->
        for (i in testDataOutput.indices) {
            assertEquals(
                testDataInput[i],
                json.encodeToString(WithChoices.serializer(), testDataOutput[i], streaming),
                "failed test on ${testDataOutput[i]}, jsonTestingMode = $streaming"
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
        override fun selectDeserializer(element: JsonElement) = when {
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
}
