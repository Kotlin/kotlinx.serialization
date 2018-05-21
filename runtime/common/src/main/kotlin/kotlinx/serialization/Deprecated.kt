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

private const val deprecationText = "Obsolete name from preview version of library."

@Deprecated(deprecationText, ReplaceWith("SerialKind"))
typealias KSerialClassKind = SerialKind

@Deprecated(deprecationText, ReplaceWith("SerialDescriptor"))
typealias KSerialClassDesc = SerialDescriptor

@Deprecated(deprecationText, ReplaceWith("SerializationStrategy<T>"))
typealias KSerialSaver<T> = SerializationStrategy<T>

@Deprecated(deprecationText, ReplaceWith("DeserializationStrategy<T>"))
typealias KSerialLoader<T> = DeserializationStrategy<T>

@Deprecated(deprecationText, ReplaceWith("CompositeEncoder"))
typealias KOutput = CompositeEncoder

@Deprecated(deprecationText, ReplaceWith("CompositeDecoder"))
typealias KInput = CompositeDecoder
