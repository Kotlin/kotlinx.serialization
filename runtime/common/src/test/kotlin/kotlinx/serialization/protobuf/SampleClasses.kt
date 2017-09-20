package kotlinx.serialization.protobuf

import kotlinx.serialization.Optional
import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import kotlin.test.assertEquals

@Serializable
data class TestInt(@SerialId(1) @ProtoType(ProtoNumberType.SIGNED) val a: Int)

@Serializable
data class TestList(@SerialId(1) @Optional val a: List<Int> = emptyList())

@Serializable
data class TestString(@SerialId(2) val b: String)

@Serializable
data class TestInner(@SerialId(3) val a: TestInt)

@Serializable
data class TestComplex(@SerialId(42) val b: Int, @SerialId(2) val c: String)

@Serializable
data class TestNumbers(@SerialId(1) @ProtoType(ProtoNumberType.FIXED) val a: Int, @SerialId(2) val b: Long)

infix fun <T> T.shouldBe(expected: T) = assertEquals(expected, this)

val t1 = TestInt(-150)
val t1e = TestInt(0)
val t2 = TestList(listOf(150, 228, 1337))
val t2e = TestList(listOf())
val t3 = TestString("testing")
val t3e = TestString("")
val t4 = TestInner(t1)
val t5 = TestComplex(42, "testing")
val t6 = TestNumbers(100500, Long.MAX_VALUE)