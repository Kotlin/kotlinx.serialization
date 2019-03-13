/*
 * Copyright 2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import kotlin.test.assertEquals

@Serializable
data class TestInt(@SerialId(1) @ProtoType(ProtoNumberType.SIGNED) val a: Int)

@Serializable
data class TestList(@SerialId(1) val a: List<Int> = emptyList())

@Serializable
data class TestString(@SerialId(2) val b: String)

@Serializable
data class TestInner(@SerialId(3) val a: TestInt)

@Serializable
data class TestComplex(@SerialId(42) val b: Int, @SerialId(2) val c: String)

@Serializable
data class TestNumbers(@SerialId(1) @ProtoType(ProtoNumberType.FIXED) val a: Int, @SerialId(2) val b: Long)

@Serializable
data class TestIntWithList(
        @SerialId(1) val s: Int,
        @SerialId(10) val l: List<Int>
)

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
