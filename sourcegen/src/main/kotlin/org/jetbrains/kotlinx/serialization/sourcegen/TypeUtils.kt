/*
 *  Copyright 2018 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.jetbrains.kotlinx.serialization.sourcegen

import com.squareup.kotlinpoet.ClassName

const val internalPackageFqName = "kotlinx.serialization.internal"

class SerialTypeInfo(
        val elementMethodPrefix: String,
        val serializer: ClassName? = null,
        val unit: Boolean = false
)

internal fun SClass.SProperty.getSerialTypeInfo(): SerialTypeInfo {
    val T = type as? ClassName ?: TODO("Generic and other complex types not supported yet")
    val kBaseClass = Regex("kotlin.(\\w+)").matchEntire(T.canonicalName)?.groupValues?.getOrNull(1)
    return when {
        !T.nullable && kBaseClass == "Unit" -> SerialTypeInfo(kBaseClass, null, true)
        !T.nullable && kBaseClass in listOf("Boolean", "Byte", "Short", "Int", "Long", "Char", "String") -> SerialTypeInfo(kBaseClass!!, null, false)
        else -> {
            val stdSerial = findStandardKotlinTypeSerializer(T.asNonNullable().canonicalName)
            if (stdSerial != null) SerialTypeInfo(if (T.nullable) "NullableSerializable" else "Serializable", stdSerial, false)
            else SerialTypeInfo(if (T.nullable) "NullableSerializable" else "Serializable", T.nestedClass("serializer"), false)
        }
    }
}

internal fun findStandardKotlinTypeSerializer(canonicalName: String): ClassName? {
    val name = when (canonicalName) {
        "kotlin.Unit" -> "UnitSerializer"
        "Z", "kotlin.Boolean" -> "BooleanSerializer"
        "B", "kotlin.Byte" -> "ByteSerializer"
        "S", "kotlin.Short" -> "ShortSerializer"
        "I", "kotlin.Int" -> "IntSerializer"
        "J", "kotlin.Long" -> "LongSerializer"
        "F", "kotlin.Float" -> "FloatSerializer"
        "D", "kotlin.Double" -> "DoubleSerializer"
        "C", "kotlin.Char" -> "CharSerializer"
        "kotlin.String" -> "StringSerializer"
        "kotlin.Pair" -> "PairSerializer"
        "kotlin.Triple" -> "TripleSerializer"
        "kotlin.collections.Collection", "kotlin.collections.List",
        "kotlin.collections.ArrayList", "kotlin.collections.MutableList" -> "ArrayListSerializer"
        "kotlin.collections.Set", "kotlin.collections.LinkedHashSet", "kotlin.collections.MutableSet" -> "LinkedHashSetSerializer"
        "kotlin.collections.HashSet" -> "HashSetSerializer"
        "kotlin.collections.Map", "kotlin.collections.LinkedHashMap", "kotlin.collections.MutableMap" -> "LinkedHashMapSerializer"
        "kotlin.collections.HashMap" -> "HashMapSerializer"
        "kotlin.collections.Map.Entry" -> "MapEntrySerializer"
        else -> return null
    }
    return ClassName(internalPackageFqName, name)
}