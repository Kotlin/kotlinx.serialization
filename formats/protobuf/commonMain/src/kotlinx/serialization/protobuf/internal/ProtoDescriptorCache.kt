/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalSerializationApi::class)

package kotlinx.serialization.protobuf.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.modules.*
import kotlin.jvm.*

/**
 * Per-[ProtoBuf][kotlinx.serialization.protobuf.ProtoBuf] cache of metadata derived from descriptor annotations.
 * Values are immutable; concurrent misses may compute the same value twice.
 *
 * Descriptor equality ignores annotations, so two different classes with the same serial name and shape
 * share a key. Every entry remembers the descriptor it was computed from and is reused for any other
 * descriptor only if the proto tags of all its elements are the same.
 */
internal class ProtoDescriptorCache {
    internal val decoding: DescriptorMap<ProtoDecodingInfo> = createDescriptorMap()
    internal val encoding: DescriptorMap<EncodingTags> = createDescriptorMap()

    fun decodingInfo(descriptor: SerialDescriptor, serializersModule: SerializersModule): ProtoDecodingInfo {
        val cached = decoding[descriptor]
        if (cached != null && cached.appliesTo(descriptor)) return cached
        val info = ProtoDecodingInfo.build(descriptor, serializersModule)
        if (cached == null) decoding.putIfAbsent(descriptor, info)
        return info
    }

    /**
     * Returns `null` if some element has an invalid proto number,
     * so the error is still thrown by [extractParameters] when that element is encoded.
     */
    fun encodingTags(descriptor: SerialDescriptor): LongArray? {
        val cached = encoding[descriptor]
        if (cached != null && cached.appliesTo(descriptor)) return cached.tags
        val tags = computeTags(descriptor)
        if (cached == null) {
            encoding.putIfAbsent(descriptor, if (tags == null) EncodingTags.Invalid(descriptor) else EncodingTags.Valid(descriptor, tags))
        }
        return tags
    }
}

internal sealed class EncodingTags(@JvmField protected val source: SerialDescriptor) {
    abstract val tags: LongArray?
    abstract fun appliesTo(descriptor: SerialDescriptor): Boolean

    class Valid(source: SerialDescriptor, override val tags: LongArray) : EncodingTags(source) {
        override fun appliesTo(descriptor: SerialDescriptor): Boolean =
            descriptor === source || descriptor.hasTags(source, tags)
    }

    class Invalid(source: SerialDescriptor) : EncodingTags(source) {
        override val tags: LongArray? get() = null
        override fun appliesTo(descriptor: SerialDescriptor): Boolean = descriptor === source
    }
}

internal class ProtoDecodingInfo(
    private val source: SerialDescriptor,
    @JvmField val tags: LongArray?,
    // Proto id -> index in serial descriptor
    @JvmField val indexCache: IntArray?,
    @JvmField val sparseIndexCache: Map<Int, Int>?,
    @JvmField val unknownHolderIndex: Int,
    // Oneof and unknown fields holder elements, whose proto ids are known only at runtime
    @JvmField val runtimeIdElements: Int,
) {
    fun appliesTo(descriptor: SerialDescriptor): Boolean {
        if (descriptor === source) return true
        val tags = tags ?: return false
        return tags.none { it.isOneOf } && descriptor.hasTags(source, tags)
    }

    companion object {
        fun build(descriptor: SerialDescriptor, serializersModule: SerializersModule): ProtoDecodingInfo {
            val elements = descriptor.elementsCount
            if (elements < 32) {
                /*
                 * If we have reasonably small count of elements, try to build sequential
                 * array for the fast-path. Fast-path implies that elements are not marked with @ProtoId
                 * explicitly or are monotonic and incremental (maybe, 1-indexed)
                 *
                 * Initialize all elements, because there will always be one extra element as arrays are numbered from 0
                 * but in protobuf field number starts from 1.
                 */
                val cache = IntArray(elements + 1) { INDEX_NOT_EXISTED }
                var dense = true
                for (i in 0 until elements) {
                    val protoId = extractProtoId(descriptor, i, false)
                    // If any element is marked as ProtoOneOf or Unknown field holder,
                    // the fast path is not applicable
                    // because num of id does not match the elements
                    if (protoId in 0..elements) {
                        cache[protoId] = i
                    } else {
                        dense = false
                        break
                    }
                }
                if (dense) return ProtoDecodingInfo(descriptor, computeTags(descriptor), cache, null, INDEX_NOT_EXISTED, 0)
            }
            return buildSparse(descriptor, elements, serializersModule)
        }

        private fun buildSparse(
            descriptor: SerialDescriptor,
            elements: Int,
            serializersModule: SerializersModule
        ): ProtoDecodingInfo {
            val map = HashMap<Int, Int>(elements, 1f)
            var unknownHolderIndex = INDEX_NOT_EXISTED
            var runtimeIdElements = 0
            for (i in 0 until elements) {
                val id = extractProtoId(descriptor, i, false)
                when (id) {
                    ID_HOLDER_ONE_OF -> {
                        descriptor.getElementDescriptor(i)
                            .getAllOneOfSerializerOfField(serializersModule)
                            .map { it.extractParameters(0).protoId }
                            .forEach { map[it] = i }
                        runtimeIdElements++
                    }

                    ID_HOLDER_UNKNOWN_FIELDS -> {
                        require(unknownHolderIndex == INDEX_NOT_EXISTED) {
                            "Only one unknown fields holder is allowed in a message, but two properties have ProtoUnknownFieldHolder type: ${descriptor.getElementName(i)} and ${descriptor.getElementName(unknownHolderIndex)}"
                        }
                        runtimeIdElements++
                        unknownHolderIndex = i
                    }

                    else -> map[id] = i
                }
            }
            return ProtoDecodingInfo(descriptor, computeTags(descriptor), null, map, unknownHolderIndex, runtimeIdElements)
        }
    }
}

private fun computeTags(descriptor: SerialDescriptor): LongArray? = try {
    LongArray(descriptor.elementsCount) { descriptor.extractParameters(it) }
} catch (_: SerializationException) {
    null
}

private fun SerialDescriptor.hasTags(source: SerialDescriptor, tags: LongArray): Boolean {
    if (elementsCount != tags.size) return false
    try {
        for (i in tags.indices) {
            if (getElementDescriptor(i) === source.getElementDescriptor(i) &&
                getElementAnnotations(i) === source.getElementAnnotations(i)
            ) continue
            if (extractParameters(i) != tags[i]) return false
        }
    } catch (_: SerializationException) {
        return false
    }
    return true
}

internal const val MAX_CACHED_DESCRIPTORS = 4096

/**
 * Thread-safe map that stops accepting new entries once it holds [MAX_CACHED_DESCRIPTORS] of them.
 */
internal interface DescriptorMap<V : Any> {
    operator fun get(descriptor: SerialDescriptor): V?
    fun putIfAbsent(descriptor: SerialDescriptor, value: V)
    val size: Int
}

internal expect fun <V : Any> createDescriptorMap(): DescriptorMap<V>
