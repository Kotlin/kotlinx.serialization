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

package kotlinx.serialization.sourcegen

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName

const val internalPackageFqName = "kotlinx.serialization.internal"

class SerialTypeInfo(
        val elementMethodPrefix: String,
        val serializer: TypeName? = null,
        val needTypeParam: Boolean = false
)

internal fun SClass.SProperty.getSerialTypeInfo(): SerialTypeInfo {
    val kBaseClass = (type as? ClassName)?.let { Regex("kotlin.(\\w+)").matchEntire(it.canonicalName)?.groupValues?.getOrNull(1) }
    return when {
        isEnum -> {
            var serializer: TypeName = ClassName(internalPackageFqName, "ModernEnumSerializer")
            if (type.nullable) serializer = serializer.wrapInNullableSerializer()
            SerialTypeInfo(if (type.nullable) "NullableSerializable" else "Serializable",
                    serializer, needTypeParam = true)
        }
        !type.nullable && kBaseClass in listOf("Unit", "Boolean", "Byte", "Short", "Int", "Long", "Char", "String") -> SerialTypeInfo(kBaseClass!!, null)
        else -> SerialTypeInfo(if (type.nullable) "NullableSerializable" else "Serializable", type.getSerializerTypeName())
    }
}

private fun TypeName.wrapInNullableSerializer(): TypeName =
        ParameterizedTypeName.get(ClassName(internalPackageFqName, "NullableSerializer"), this)


private val TypeName.erasedType: ClassName
    get() = when (this) {
        is ClassName -> this
        is ParameterizedTypeName -> this.rawType
        else -> TODO("Too complex type")
    }

internal fun TypeName.getSerializerTypeName(): TypeName {
    val typeName = when (this) {
        is ClassName -> findStandardKotlinTypeSerializer(canonicalName) ?: nestedClass("serializer")
        is ParameterizedTypeName -> ParameterizedTypeName.get(
                rawType.asNonNullable().getSerializerTypeName().erasedType,
                *(typeArguments.map(TypeName::getSerializerTypeName).toTypedArray())
        )
        else -> TODO("Too complex type")
    }
    return if (nullable) typeName.wrapInNullableSerializer()
    else typeName
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
