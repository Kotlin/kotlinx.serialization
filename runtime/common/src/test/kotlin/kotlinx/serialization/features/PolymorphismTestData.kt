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

package kotlinx.serialization.features

import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule

@Serializable
open class PolyBase(@SerialId(1) val id: Int) {
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
data class PolyDerived(@SerialId(2) val s: String) : PolyBase(1)

val BaseAndDerivedModule = SerializersModule {
    polymorphic(PolyBase::class, PolyBase.serializer()) {
        PolyDerived::class with PolyDerived.serializer()
    }
}
