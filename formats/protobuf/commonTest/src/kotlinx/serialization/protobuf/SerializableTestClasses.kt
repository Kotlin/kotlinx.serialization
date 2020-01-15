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

import kotlinx.serialization.Serializable

@Serializable
data class TestInt(@ProtoId(1) @ProtoType(ProtoNumberType.SIGNED) val a: Int)

@Serializable
data class TestList(@ProtoId(1) val a: List<Int> = emptyList())

@Serializable
data class TestString(@ProtoId(2) val b: String)

@Serializable
data class TestInner(@ProtoId(3) val a: TestInt)

@Serializable
data class TestComplex(@ProtoId(42) val b: Int, @ProtoId(2) val c: String)

@Serializable
data class TestNumbers(@ProtoId(1) @ProtoType(ProtoNumberType.FIXED) val a: Int, @ProtoId(2) val b: Long)

@Serializable
data class TestIntWithList(
    @ProtoId(1) val s: Int,
    @ProtoId(10) val l: List<Int>
)
