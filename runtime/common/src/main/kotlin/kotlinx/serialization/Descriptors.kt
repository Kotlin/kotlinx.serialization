/*
 * Copyright 2018 JetBrains s.r.o.
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

package kotlinx.serialization

import kotlinx.serialization.internal.PrimitiveDescriptor

sealed class SerialKind

sealed class PrimitiveKind: SerialKind() {
    object INT : PrimitiveKind()
    object UNIT : PrimitiveKind()
    object BOOLEAN : PrimitiveKind()
    object BYTE : PrimitiveKind()
    object SHORT : PrimitiveKind()
    object LONG : PrimitiveKind()
    object FLOAT : PrimitiveKind()
    object DOUBLE : PrimitiveKind()
    object CHAR : PrimitiveKind()
    object STRING: PrimitiveKind()
}

sealed class StructureKind: SerialKind() {
    object CLASS: StructureKind()
    object LIST: StructureKind()
    object MAP: StructureKind()
}

sealed class UnionKind: SerialKind() {
    object OBJECT: UnionKind()
    object ENUM_KIND: UnionKind() // https://github.com/JetBrains/kotlin-native/issues/1447
    object SEALED: UnionKind()
    object POLYMORPHIC: UnionKind()
}

class PrimitiveDescriptorWithName(override val name: String, val original: PrimitiveDescriptor) :
    SerialDescriptor by original

fun PrimitiveDescriptor.withName(name: String): SerialDescriptor = PrimitiveDescriptorWithName(name, this)
