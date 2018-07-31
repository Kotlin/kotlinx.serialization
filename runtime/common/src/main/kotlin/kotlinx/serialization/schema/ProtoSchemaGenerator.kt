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

@file:Suppress("FunctionName")

package kotlinx.serialization.schema

import kotlinx.serialization.*
import kotlinx.serialization.protobuf.ProtoNumberType
import kotlinx.serialization.protobuf.extractParameters

fun ProtoSchema(descriptor: SerialDescriptor): ProtoMessage {
    val fields = descriptor.elementDescriptors().mapIndexed { i, child ->
        val (id, numberAnnotation) = extractParameters(descriptor, i)
        val rule = when {
            child.kind is StructureKind.LIST -> ProtoFieldRule.REPEATED
            child.kind is StructureKind.MAP -> ProtoFieldRule.MAP
            descriptor.isElementOptional(i) -> ProtoFieldRule.OPTIONAL
            else -> ProtoFieldRule.REQUIRED
        }
        val name = descriptor.getElementName(i)
        val type = child.accept(ProtoTypeInference(numberAnnotation))
        ProtoField(id, name, rule, type)
    }.toList()
    return ProtoMessage(descriptor.name, fields)
}


private class ProtoTypeInference(val protoNumberType: ProtoNumberType) : BaseDescriptorVisitor<ProtoType>() {
    override fun visitString(descriptor: SerialDescriptor): ProtoType = LengthDelimited.string

    override fun visitPrimitive(descriptor: SerialDescriptor): ProtoType = when (descriptor.kind) {
        is PrimitiveKind.BYTE, PrimitiveKind.SHORT, PrimitiveKind.INT, PrimitiveKind.CHAR -> when (protoNumberType) {
            ProtoNumberType.DEFAULT -> VarintType.int32
            ProtoNumberType.SIGNED -> VarintType.sint32
            ProtoNumberType.FIXED -> Fixed32BitType.fixed32
        }
        is PrimitiveKind.LONG -> when (protoNumberType) {
            ProtoNumberType.DEFAULT -> VarintType.int64
            ProtoNumberType.SIGNED -> VarintType.sint64
            ProtoNumberType.FIXED -> Fixed64BitType.fixed64
        }
        is PrimitiveKind.BOOLEAN -> VarintType.bool
        is PrimitiveKind.FLOAT -> Fixed32BitType.float
        is PrimitiveKind.DOUBLE -> Fixed64BitType.double
        is PrimitiveKind.UNIT -> throw UnsupportedOperationException()
        else -> throw AssertionError()
    }

    override fun visitClass(descriptor: SerialDescriptor): ProtoType {
        return ProtoSchema(descriptor)
    }

    override fun visitCollection(descriptor: SerialDescriptor): ProtoType {
        return when (descriptor.kind) {
            is StructureKind.LIST -> visitDescriptor(descriptor.getElementDescriptor(0))
            is StructureKind.MAP -> ProtoMap(
                visitDescriptor(descriptor.getElementDescriptor(0)),
                visitDescriptor(descriptor.getElementDescriptor(1))
            )
            else -> throw AssertionError()
        }
    }

    override fun visitEnum(descriptor: SerialDescriptor): ProtoType {
        return VarintType.enum
    }

    override fun visitUnion(descriptor: SerialDescriptor): ProtoType {
        throw NotImplementedError("Unions are not supported in protobuf")
    }
}
