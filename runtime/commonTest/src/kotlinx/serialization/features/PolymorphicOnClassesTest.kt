/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.reflect.*
import kotlin.test.*

class PolymorphicOnClassesTest {

    // this has implicit @Polymorphic
    interface IMessage {
        val body: String
    }

    // and this class too has implicit @Polymorphic
    @Serializable
    abstract class Message : IMessage {
        abstract override val body: String
    }

    @Polymorphic
    @Serializable
    @SerialName("SimpleMessage") // to cut out package prefix
    open class SimpleMessage : Message() {
        override var body: String = "Simple"
    }

    @Serializable
    @SerialName("DoubleSimpleMessage")
    class DoubleSimpleMessage(val body2: String) : SimpleMessage()

    @Serializable
    @SerialName("MessageWithId")
    open class MessageWithId(val id: Int, override val body: String) : Message()

    @Serializable
    class Holder(
        val iMessage: IMessage,
        val iMessageList: List<IMessage>,
        val message: Message,
        val msgSet: Set<Message>,
        val simple: SimpleMessage,
        // all above should be polymorphic
        val withId: MessageWithId // but this not
    )


    private fun genTestData(): Holder {
        var cnt = -1
        fun gen(): MessageWithId {
            cnt++
            return MessageWithId(cnt, "Message #$cnt")
        }

        return Holder(gen(), listOf(gen(), gen()), gen(), setOf(SimpleMessage()), DoubleSimpleMessage("DoubleSimple"), gen())
    }

    @Suppress("UNCHECKED_CAST")
    private val testModule = SerializersModule {
        listOf(Message::class, IMessage::class, SimpleMessage::class).forEach { clz ->
            polymorphic(clz as KClass<IMessage>) {
                subclass(SimpleMessage.serializer())
                subclass(DoubleSimpleMessage.serializer())
                subclass(MessageWithId.serializer())
            }
        }
    }

    @Test
    fun testEnablesImplicitlyOnInterfacesAndAbstractClasses() {
        val json = Json { prettyPrint = false; useArrayPolymorphism = true; serializersModule = testModule }
        val data = genTestData()
        assertEquals(
            """{"iMessage":["MessageWithId",{"id":0,"body":"Message #0"}],""" +
                    """"iMessageList":[["MessageWithId",{"id":1,"body":"Message #1"}],""" +
                    """["MessageWithId",{"id":2,"body":"Message #2"}]],"message":["MessageWithId",{"id":3,"body":"Message #3"}],""" +
                    """"msgSet":[["SimpleMessage",{"body":"Simple"}]],"simple":["DoubleSimpleMessage",{"body":"Simple",""" +
                    """"body2":"DoubleSimple"}],"withId":{"id":4,"body":"Message #4"}}""",
            json.encodeToString(Holder.serializer(), data)
        )
    }

    @Test
    fun testDescriptor() {
        val polyDesc = Holder.serializer().descriptor.elementDescriptors.first()
        assertEquals(PolymorphicSerializer(IMessage::class).descriptor, polyDesc)
        assertEquals(2, polyDesc.elementsCount)
        assertEquals(PrimitiveKind.STRING, polyDesc.getElementDescriptor(0).kind)
    }

    private fun SerialDescriptor.inContext(module: SerializersModule): List<SerialDescriptor> = when (kind) {
        PolymorphicKind.OPEN -> module.getPolymorphicDescriptors(this)
        else -> error("Expected this function to be called on OPEN descriptor")
    }

    @Test
    fun testResolvePolymorphicDescriptor() {
        val polyDesc = Holder.serializer().descriptor.elementDescriptors.first() // iMessage: IMessage

        assertEquals(PolymorphicKind.OPEN, polyDesc.kind)

        val inheritors = polyDesc.inContext(testModule)
        val names = listOf("SimpleMessage", "DoubleSimpleMessage", "MessageWithId").toSet()
        assertEquals(names, inheritors.map(SerialDescriptor::serialName).toSet(), "Expected correct inheritor names")
        assertTrue(inheritors.all { it.kind == StructureKind.CLASS }, "Expected all inheritors to be CLASS")
    }

    @Test
    fun testDocSampleWithAllDistinct() {
        fun allDistinctNames(descriptor: SerialDescriptor, module: SerializersModule) = when (descriptor.kind) {
            is PolymorphicKind.OPEN -> module.getPolymorphicDescriptors(descriptor)
                .map { it.elementNames.toList() }.flatten().toSet()
            is SerialKind.CONTEXTUAL -> module.getContextualDescriptor(descriptor)?.elementNames?.toList().orEmpty().toSet()
            else -> descriptor.elementNames.toSet()
        }

        val polyDesc = Holder.serializer().descriptor.elementDescriptors.first() // iMessage: IMessage
        assertEquals(setOf("id", "body", "body2"), allDistinctNames(polyDesc, testModule))
        assertEquals(setOf("id", "body"), allDistinctNames(MessageWithId.serializer().descriptor, testModule))
    }
}
