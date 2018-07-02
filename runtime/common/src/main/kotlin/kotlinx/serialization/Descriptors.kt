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

sealed class SerialKind

sealed class PrimitiveKind: SerialKind() {
    object PRIMITIVE: PrimitiveKind() // TODO: remove it later
    object INT : PrimitiveKind()
    object STRING: PrimitiveKind()
}

sealed class StructureKind: SerialKind() {
    object CLASS: StructureKind()
    object LIST: StructureKind()
    object SET: StructureKind() // do we need it?
    object MAP: StructureKind()
    object ENTRY: StructureKind() // TODO: remove it when formats would support new map representation
}

sealed class UnionKind: SerialKind() {
    object OBJECT: UnionKind()
    object ENUM_KIND: UnionKind() // https://github.com/JetBrains/kotlin-native/issues/1447
    object SEALED: UnionKind()
    object POLYMORPHIC: UnionKind()
}

sealed class PrimitiveDescriptor(override val name: String, override val kind: PrimitiveKind): SerialDescriptor {
    final override fun getElementName(index: Int): String {
        throw IllegalStateException("Primitives does not have elements")
    }

    override fun getElementIndex(name: String): Int {
        throw IllegalStateException("Primitives does not have elements")
    }
}

object IntDescriptor: PrimitiveDescriptor("kotlin.Int", PrimitiveKind.INT) // or just "Int"?
object StringDescriptor: PrimitiveDescriptor("kotlin.String", PrimitiveKind.STRING) // or just "Int"?
