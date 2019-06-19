/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlin.jvm.JvmOverloads
import kotlin.reflect.KClass

public class EnumDescriptor @JvmOverloads constructor(
    override val name: String,
    private val choices: Array<String> = emptyArray()
) : SerialClassDescImpl(name) {
    override val kind: SerialKind = UnionKind.ENUM_KIND

    init {
        choices.forEach { addElement(it) }
    }

    override fun getElementDescriptor(index: Int): SerialDescriptor {
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as EnumDescriptor

        if (name != other.name) return false
        if (elementNames() != other.elementNames()) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + choices.contentHashCode()
        return result
    }
}

open class CommonEnumSerializer<T>(val serialName: String, val choices: Array<T>, choicesNames: Array<String>) :
    KSerializer<T> {
    override val descriptor: EnumDescriptor = EnumDescriptor(serialName, choicesNames)

    final override fun serialize(encoder: Encoder, obj: T) {
        val index = choices.indexOf(obj)
            .also { check(it != -1) { "$obj is not a valid enum $serialName, choices are $choices" } }
        encoder.encodeEnum(descriptor, index)
    }

    final override fun deserialize(decoder: Decoder): T {
        val index = decoder.decodeEnum(descriptor)
        check(index in choices.indices)
            { "$index is not among valid $serialName choices, choices size is ${choices.size}" }
        return choices[index]
    }
}

// Binary backwards-compatible with plugin
class EnumSerializer<T : Enum<T>> @JvmOverloads constructor(
    serializableClass: KClass<T>,
    serialName: String = serializableClass.enumClassName()
) : CommonEnumSerializer<T>(
    serialName,
    serializableClass.enumMembers(),
    serializableClass.enumMembers().map { it.name }.toTypedArray()
)
