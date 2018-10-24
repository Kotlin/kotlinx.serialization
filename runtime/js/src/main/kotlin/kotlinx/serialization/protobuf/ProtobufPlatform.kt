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

package kotlinx.serialization.protobuf

import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.SerialId
import kotlinx.serialization.internal.onlySingleOrNull

internal actual fun extractParameters(desc: SerialDescriptor, index: Int): ProtoDesc {
    val tag = desc.getElementAnnotations(index).filterIsInstance<SerialId>().onlySingleOrNull()?.id ?: index
    val format = desc.getElementAnnotations(index).filterIsInstance<ProtoType>().onlySingleOrNull()?.type
            ?: ProtoNumberType.DEFAULT
    return tag to format
}
