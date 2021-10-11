/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package sample

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlinx.serialization.protobuf.ProtoNumber

@Serializable
data class IntData(val intV: Int)

@Serializable
data class Box<T>(val boxed: T)

@Serializable
data class HasBox<T>(val desc: String, val boxes: List<Box<T>>)

@Serializable
data class Data(
    val s: String,
    val box: Box<Int> = Box(42),
    val boxes: HasBox<String> = HasBox("boxes", listOf(Box("foo"), Box("bar"))),
    val m: Map<Int, String> = emptyMap()
)

// this has implicit @Polymorphic
interface IMessage {
    val body: String
}

// and this class too has implicit @Polymorphic
@Serializable
abstract class Message() : IMessage {
    abstract override val body: String
}

@Polymorphic
@Serializable
@SerialName("SimpleMessage") // to cut out package prefix
open class SimpleMessage() : Message() {
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

@Serializable
class GenericMessage<T : IMessage, V: Any>(
    @Polymorphic val value: T,
    @Polymorphic val value2: V
)

@Serializable
abstract class AbstractSerializable {
    public abstract val rootState: String // no backing field

    val publicState: String = "A"
}

@Serializable
open class SerializableBase: AbstractSerializable() {


    private val privateState: String = "B" // still should be serialized

    @Transient
    private val privateTransientState = "C" // not serialized: explicitly transient

    val notAState: String // not serialized: no backing field
        get() = "D"

    override val rootState: String
        get() = "E" // still not serializable

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SerializableBase) return false

        if (privateState != other.privateState) return false
        if (privateTransientState != other.privateTransientState) return false

        return true
    }
}

@Serializable
class Derived(val derivedState: Int): SerializableBase() {
    override val rootState: String = "foo" // serializable!

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Derived) return false
        if (!super.equals(other)) return false

        if (derivedState != other.derivedState) return false
        if (rootState != other.rootState) return false

        return true
    }
}

@Serializable
open class Base1(open var state1: String) {
    override fun toString(): String {
        return "Base1(state1='$state1')"
    }
}

@Serializable
class Derived2(@SerialName("state2") override var state1: String): Base1(state1) {
    override fun toString(): String {
        return "Derived2(state1='$state1')"
    }
}

@Serializable
open class PolyBase(@ProtoNumber(1) val id: Int) {
    override fun hashCode(): Int {
        return id
    }

    override fun toString(): String {
        return "PolyBase(id=$id)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as PolyBase

        if (id != other.id) return false

        return true
    }

}

@Serializable
data class PolyDerived(@ProtoNumber(2) val s: String) : PolyBase(1)

val BaseAndDerivedModule = SerializersModule {
    polymorphic(PolyBase::class, PolyBase.serializer()) {
        subclass(PolyDerived.serializer())
    }
}

@Serializable
data class MyPolyData(val data: Map<String, @Polymorphic Any>)

@Serializable
data class MyPolyDataWithPolyBase(
    val data: Map<String, @Polymorphic Any>,
    @Polymorphic val polyBase: PolyBase
)

enum class Attitude { POSITIVE, NEUTRAL, NEGATIVE }

@Serializable
data class Tree(val name: String, val left: Tree? = null, val right: Tree? = null)

@Serializable
data class Zoo(
    val unit: Unit,
    val boolean: Boolean,
    val byte: Byte,
    val short: Short,
    val int: Int,
    val long: Long,
    val float: Float,
    val double: Double,
    val char: Char,
    val string: String,
    val enum: Attitude,
    val intData: IntData,
    val unitN: Unit?,
    val booleanN: Boolean?,
    val byteN: Byte?,
    val shortN: Short?,
    val intN: Int?,
    val longN: Long?,
    val floatN: Float?,
    val doubleN: Double?,
    val charN: Char?,
    val stringN: String?,
    val enumN: Attitude?,
    val intDataN: IntData?,
    val listInt: List<Int>,
    val listIntN: List<Int?>,
    val listNInt: Set<Int>?,
    val listNIntN: MutableSet<Int?>?,
    val listListEnumN: List<List<Attitude?>>,
    val listIntData: List<IntData>,
    val listIntDataN: MutableList<IntData?>,
    val tree: Tree,
    val mapStringInt: Map<String,Int>,
    val mapIntStringN: Map<Int,String?>,
    val arrays: ZooWithArrays
)

@Serializable
data class ZooWithArrays(
    val arrByte: Array<Byte>,
    val arrInt: Array<Int>,
    val arrIntN: Array<Int?>,
    val arrIntData: Array<IntData>

) {
    override fun equals(other: Any?) = other is ZooWithArrays &&
            arrByte.contentEquals(other.arrByte) &&
            arrInt.contentEquals(other.arrInt) &&
            arrIntN.contentEquals(other.arrIntN) &&
            arrIntData.contentEquals(other.arrIntData)
}

val zoo = Zoo(
    Unit, true, 10, 20, 30, 40, 50f, 60.0, 'A', "Str0", Attitude.POSITIVE, IntData(70),
    null, null, 11, 21, 31, 41, 51f, 61.0, 'B', "Str1", Attitude.NEUTRAL, null,
    listOf(1, 2, 3),
    listOf(4, 5, null),
    setOf(6, 7, 8),
    mutableSetOf(null, 9, 10),
    listOf(listOf(Attitude.NEGATIVE, null)),
    listOf(IntData(1), IntData(2), IntData(3)),
    mutableListOf(IntData(1), null, IntData(3)),
    Tree("root", Tree("left"), Tree("right", Tree("right.left"), Tree("right.right"))),
    mapOf("one" to 1, "two" to 2, "three" to 3),
    mapOf(0 to null, 1 to "first", 2 to "second"),
    ZooWithArrays(
        arrayOf(1, 2, 3),
        arrayOf(100, 200, 300),
        arrayOf(null, -1, -2),
        arrayOf(IntData(1), IntData(2))
    )
)
