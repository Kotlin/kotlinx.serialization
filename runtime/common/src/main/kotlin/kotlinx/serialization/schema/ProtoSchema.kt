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

@file:Suppress("EnumEntryName")

package kotlinx.serialization.schema

import kotlinx.serialization.protobuf.ProtoBuf

interface ProtoType {
    val name: String
    val wireType: Int
}

data class ProtoField(val protoId: Int, val name: String, val rule: ProtoFieldRule, val type: ProtoType) {
    override fun toString(): String {
        val prefix = if (rule.displayName.isNotEmpty()) "${rule.displayName} ${type.name}" else type.name
        return "$prefix $name = $protoId;"
    }
}

data class ProtoMessage(override val name: String, val fields: List<ProtoField>): ProtoType {
    override val wireType: Int = ProtoBuf.SIZE_DELIMITED

    override fun toString(): String {
        return "message $name {" + fields.joinToString("\n  ", "\n  ", "\n") + "}"
    }
}

data class ProtoMap(val keyType: ProtoType, val valueType: ProtoType) : ProtoType {
    override val name: String = "map<${keyType.name}, ${valueType.name}>"
    override val wireType: Int = ProtoBuf.SIZE_DELIMITED

    override fun toString(): String = name
}

enum class ProtoFieldRule {
    REQUIRED,
    OPTIONAL,
    REPEATED,
    MAP {
        override val displayName: String get() = ""
    };

    open val displayName: String
        get() = name.toLowerCase()
}

enum class VarintType: ProtoType {
    int32, int64, sint32, sint64, bool, enum;

    override val wireType: Int = ProtoBuf.VARINT
}

enum class Fixed64BitType: ProtoType {
    fixed64, double;

    override val wireType: Int = ProtoBuf.i64
}

enum class Fixed32BitType: ProtoType {
    fixed32, float;

    override val wireType: Int = ProtoBuf.i32
}

enum class LengthDelimited: ProtoType {
    string, bytes;

    override val wireType: Int = ProtoBuf.SIZE_DELIMITED
}
