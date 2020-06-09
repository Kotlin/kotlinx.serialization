/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:UseExperimental(UnstableDefault::class)

package sample

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.test.*

class JsonTest {

    private val originalData = Data("Hello")
    private val originalString = """{"s":"Hello","box":{"boxed":42},"boxes":{"desc":"boxes","boxes":[{"boxed":"foo"},{"boxed":"bar"}]},"m":{}}"""
    private val nonstrict: Json = Json(
        JsonConfiguration(
            isLenient = true,
            ignoreUnknownKeys = true,
            serializeSpecialFloatingPointValues = true,
            useArrayPolymorphism = true
        )
    )
    
    @Test
    fun testStringForm() {
        val str = Json.stringify(Data.serializer(), originalData)
        assertEquals(originalString, str)
    }

    @Test
    fun testSerializeBack() {
        val restored = Json.parse(Data.serializer(), originalString)
        assertEquals(originalData, restored)
    }

    private fun genTestData(): Holder {
        var cnt = -1
        fun gen(): MessageWithId {
            cnt++
            return MessageWithId(cnt, "Message #$cnt")
        }

        return Holder(gen(), listOf(gen(), gen()), gen(), setOf(SimpleMessage()), DoubleSimpleMessage("DoubleSimple"), gen())
    }

    private val testModule = SerializersModule {
        polymorphic(Message::class, IMessage::class, SimpleMessage::class) {
            addSubclass(SimpleMessage::class, SimpleMessage.serializer())
            addSubclass(DoubleSimpleMessage::class, DoubleSimpleMessage.serializer())
            addSubclass(MessageWithId::class, MessageWithId.serializer())
        }
    }

    @Test
    fun testEnablesImplicitlyOnInterfacesAndAbstractClasses() {
        val json = Json { useArrayPolymorphism = true; unquotedPrint = true; prettyPrint = false; serialModule = testModule }
        val data = genTestData()
        assertEquals("""{iMessage:[MessageWithId,{id:0,body:"Message #0"}],iMessageList:[[MessageWithId,{id:1,body:"Message #1"}],[MessageWithId,{id:2,body:"Message #2"}]],message:[MessageWithId,{id:3,body:"Message #3"}],msgSet:[[SimpleMessage,{body:Simple}]],simple:[DoubleSimpleMessage,{body:Simple,body2:DoubleSimple}],withId:{id:4,body:"Message #4"}}""", json.stringify(Holder.serializer(), data))
    }

    @Test
    fun polymorphicForGenericUpperBound() {
        val generic = GenericMessage<Message, Any>(MessageWithId(42, "body"), "body2")
        val serial = GenericMessage.serializer(Message.serializer(), Int.serializer() as KSerializer<Any>)
        val json = Json { useArrayPolymorphism = true; unquotedPrint = true; prettyPrint = false; serialModule = testModule }
        val s = json.stringify(serial, generic)
        assertEquals("""{value:[MessageWithId,{id:42,body:body}],value2:[kotlin.String,body2]}""", s)
    }

    @Test
    fun testDescriptor() {
        val desc = Holder.serializer().descriptor
        assertEquals(PolymorphicSerializer(IMessage::class).descriptor, desc.getElementDescriptor(0))
    }

    @Test
    fun canBeSerializedAsDerived() {
        val derived = Derived(42)
        val msg = Json.stringify(Derived.serializer(), derived)
        assertEquals("""{"publicState":"A","privateState":"B","derivedState":42,"rootState":"foo"}""", msg)
        val d2 = Json.parse(Derived.serializer(), msg)
        assertEquals(derived, d2)
    }

    @Test
    fun canBeSerializedAsParent() {
        val derived = Derived(42)
        val msg = Json.stringify(SerializableBase.serializer(), derived)
        assertEquals("""{"publicState":"A","privateState":"B"}""", msg)
        val d2 = Json.parse(SerializableBase.serializer(), msg)
        assertEquals(SerializableBase(), d2)
        // no derivedState
        assertFailsWith<MissingFieldException> { Json.parse(Derived.serializer(), msg) }
    }

    @Test
    fun testWithOpenProperty() {
        val d = Derived2("foo")
        val msgFull = Json.stringify(Derived2.serializer(), d)
        assertEquals("""{"state1":"foo","state2":"foo"}""", msgFull)
        assertEquals("""{"state1":"foo"}""", Json.stringify(Base1.serializer(), d))
        val restored = Json.parse(Derived2.serializer(), msgFull)
        val restored2 = Json.parse(Derived2.serializer(), """{"state1":"bar","state2":"foo"}""") // state1 is ignored anyway
        assertEquals("""Derived2(state1='foo')""", restored.toString())
        assertEquals("""Derived2(state1='foo')""", restored2.toString())
    }

    @Test
    fun withoutModules() = assertStringFormAndRestored(
        expected = """{"data":{"stringKey":["kotlin.String","string1"],"mapKey":["kotlin.collections.HashMap",[["kotlin.String","nestedKey"],["kotlin.String","nestedValue"]]],"listKey":["kotlin.collections.ArrayList",[["kotlin.String","foo"]]]}}""",
        original = MyPolyData(
            linkedMapOf(
                "stringKey" to "string1",
                "mapKey" to hashMapOf("nestedKey" to "nestedValue"),
                "listKey" to listOf("foo")
            )
        ),
        serializer = MyPolyData.serializer(),
        format = Json(JsonConfiguration(useArrayPolymorphism = true, allowStructuredMapKeys = true))
    )

    @Suppress("NAME_SHADOWING")
    private fun checkNotRegisteredMessage(className: String, scopeName: String, exception: SerializationException) {
        val expectedText =
            "is not registered for polymorphic serialization in the scope of"
        assertEquals(true, exception.message?.contains(expectedText))
    }

    @Test
    fun failWithoutModulesWithCustomClass() {
        checkNotRegisteredMessage(
            "sample.IntData", "kotlin.Any",
            assertFailsWith<SerializationException>("not registered") {
                Json.stringify(
                    MyPolyData.serializer(),
                    MyPolyData(mapOf("a" to IntData(42)))
                )
            }
        )
    }

    @Test
    fun testWithModules() {
        val json = Json {
            useArrayPolymorphism = true; serialModule = SerializersModule { polymorphic(Any::class) { IntData::class with IntData.serializer() } } }
        assertStringFormAndRestored(
            expected = """{"data":{"a":["sample.IntData",{"intV":42}]}}""",
            original = MyPolyData(mapOf("a" to IntData(42))),
            serializer = MyPolyData.serializer(),
            format = json
        )
    }

    /**
     * This test should fail because PolyDerived registered in the scope of PolyBase, not kotlin.Any
     */
    @Test
    fun failWithModulesNotInAnyScope() {
        val json = Json(context = BaseAndDerivedModule)
        checkNotRegisteredMessage(
            "sample.PolyDerived", "kotlin.Any",
            assertFailsWith<SerializationException> {
                json.stringify(
                    MyPolyData.serializer(),
                    MyPolyData(mapOf("a" to PolyDerived("foo")))
                )
            }
        )
    }

    private val baseAndDerivedModuleAtAny = SerializersModule {
        polymorphic(Any::class) {
            PolyDerived::class with PolyDerived.serializer()
        }
    }


    @Test
    fun testRebindModules() {
        val json = Json { useArrayPolymorphism = true; serialModule =  baseAndDerivedModuleAtAny }
        assertStringFormAndRestored(
            expected = """{"data":{"a":["sample.PolyDerived",{"id":1,"s":"foo"}]}}""",
            original = MyPolyData(mapOf("a" to PolyDerived("foo"))),
            serializer = MyPolyData.serializer(),
            format = json
        )
    }

    /**
     * This test should fail because PolyDerived registered in the scope of kotlin.Any, not PolyBase
     */
    @Test
    fun failWithModulesNotInParticularScope() {
        val json = Json(context = baseAndDerivedModuleAtAny)
        checkNotRegisteredMessage(
            "sample.PolyDerived", "sample.PolyBase",
            assertFailsWith<SerializationException> {
                json.stringify(
                    MyPolyDataWithPolyBase.serializer(),
                    MyPolyDataWithPolyBase(mapOf("a" to PolyDerived("foo")), PolyDerived("foo"))
                )
            }
        )
    }

    @Test
    fun testBindModules() {
        val json = Json { useArrayPolymorphism = true; serialModule = (baseAndDerivedModuleAtAny + BaseAndDerivedModule) }
        assertStringFormAndRestored(
            expected = """{"data":{"a":["sample.PolyDerived",{"id":1,"s":"foo"}]},"polyBase":["sample.PolyDerived",{"id":1,"s":"foo"}]}""",
            original = MyPolyDataWithPolyBase(mapOf("a" to PolyDerived("foo")), PolyDerived("foo")),
            serializer = MyPolyDataWithPolyBase.serializer(),
            format = json
        )
    }

    @Test
    fun geoTest() {
        val deser = nonstrict.parse(GeoCoordinate.serializer(), """{"latitude":1.0,"longitude":1.0}""")
        assertEquals(GeoCoordinate(1.0, 1.0), deser)
    }

    @Test
    fun geoTest2() {
        val deser = nonstrict.parse(GeoCoordinate.serializer(), """{}""")
        assertEquals(GeoCoordinate(0.0, 0.0), deser)
    }

    @Test
    fun geoTestValidation() {
        assertFailsWith<IllegalArgumentException> {
            nonstrict.parse(GeoCoordinate.serializer(), """{"latitude":-1.0,"longitude":1.0}""")
        }
    }
}

inline fun <reified T : Any> assertStringFormAndRestored(
    expected: String,
    original: T,
    serializer: KSerializer<T>,
    format: StringFormat = Json,
    printResult: Boolean = false
) {
    val string = format.stringify(serializer, original)
    if (printResult) println("[Serialized form] $string")
    assertEquals(expected, string)
    val restored = format.parse(serializer, string)
    if (printResult) println("[Restored form] $original")
    assertEquals(original, restored)
}
