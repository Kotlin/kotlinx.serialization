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
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName

const val internalPackageFqName = "kotlinx.serialization.internal"

//data class SerializerDescriptor(val serializer: TypeName, val needTypeParam: Boolean = false)

class SerialTypeInfo(
        val elementMethodPrefix: String,
        val serializer: TypeName? = null,
        val needTypeParam: Boolean = false
)

internal fun SClass.SProperty.getSerialTypeInfo(): SerialTypeInfo {
    val kBaseClass = (type as? ClassName)?.let { Regex("kotlin.(\\w+)").matchEntire(it.canonicalName)?.groupValues?.getOrNull(1) }
    return when {
        isEnum -> SerialTypeInfo(if (type.nullable) "NullableSerializable" else "Serializable",
                ClassName(internalPackageFqName, "ModernEnumSerializer"), needTypeParam = true)
        !type.nullable && kBaseClass in listOf("Unit", "Boolean", "Byte", "Short", "Int", "Long", "Char", "String") -> SerialTypeInfo(kBaseClass!!, null)
        else -> SerialTypeInfo(if (type.nullable) "NullableSerializable" else "Serializable", type.getSerializerTypeName())
    }
}

internal fun TypeName.getSerializerTypeName(): TypeName {
    return when (this) {
        is ClassName -> {
            val defSerial = findStandardKotlinTypeSerializer(canonicalName) ?: this.nestedClass("serializer")
            if (nullable) ParameterizedTypeName.get(ClassName(internalPackageFqName, "NullableSerializer"), defSerial)
            else defSerial
        }
        is ParameterizedTypeName -> ParameterizedTypeName.get(rawType.getSerializerTypeName() as ClassName, *(typeArguments.map(TypeName::getSerializerTypeName).toTypedArray()))
        else -> TODO("Too complex type")
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
