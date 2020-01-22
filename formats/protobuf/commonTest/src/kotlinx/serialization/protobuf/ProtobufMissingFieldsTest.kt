/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlinx.serialization.json.JsonInput
import kotlinx.serialization.json.JsonOutput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ProtobufMissingFieldsTest {

    private val buffer = byteArrayOf(10, 30, 8, 11, 16, 2, 26, 3, 115, 112, 97, 26, 6, 115, 112, 97, 95, 101, 115, 26, 2, 101, 115, 26, 5, 101, 115, 95, 101, 115, 32, 1, 16, 25)

    @Test
    fun deserialize() {
        val items = ProtoBuf.load(Items.serializer(), buffer)
        assertEquals(25, items.pageSize)
        assertFalse(items.nextPage)
        assertEquals(1, items.items.size)
        assertEquals(ItemPlatform.Android, items.items[0].platform)
        assertEquals(11, items.items[0].id)
        assertEquals(listOf("spa", "spa_es", "es", "es_es"), items.items[0].language)
        assertEquals(ItemContext.Context1, items.items[0].context)
    }

    @Test
    fun deserializeWithoutFields() {
        val items = ProtoBuf.load(ItemsWithoutPageSize.serializer(), buffer)
        assertFalse(items.nextPage)
        assertEquals(1, items.items.size)
        assertEquals(11, items.items[0].id)
        assertEquals(listOf("spa", "spa_es", "es", "es_es"), items.items[0].language)
        assertEquals(ItemContext.Context1, items.items[0].context)
    }
}


enum class ItemPlatform {
    Unknown,
    iOS,
    Android
}

enum class ItemContext {
    Unknown,
    Context1,
    Context2
}

@Serializable
data class Items(
    @ProtoId(1)
    val items: List<Item> = emptyList(),
    @ProtoId(2)
    val pageSize: Int? = null,
    @ProtoId(3)
    val nextPage: Boolean = false
)

@Serializable
data class Item(
    @ProtoId(1)
    val id: Int,
    @ProtoId(2) @Serializable(with = ItemPlatformSerializer::class)
    val platform: ItemPlatform = ItemPlatform.Unknown,
    @ProtoId(3)
    val language: List<String> = emptyList(),
    @ProtoId(4) @Serializable(with = ItemContextSerializer::class)
    val context: ItemContext = ItemContext.Unknown
)

@Serializable
data class ItemsWithoutPageSize(
    @ProtoId(1)
    val items: List<ItemWithoutPlatform> = emptyList(),
    @ProtoId(3)
    val nextPage: Boolean = false
)

@Serializable
data class ItemWithoutPlatform(
    @ProtoId(1)
    val id: Int,
    @ProtoId(3)
    val language: List<String> = emptyList(),
    @ProtoId(4) @Serializable(with = ItemContextSerializer::class)
    val context: ItemContext = ItemContext.Unknown
)

class ItemPlatformSerializer : KSerializer<ItemPlatform> {

    override val descriptor: SerialDescriptor = SerialClassDescImpl("ItemPlatform")

    override fun deserialize(decoder: Decoder): ItemPlatform {
        if (decoder is JsonInput) {
            val str = decoder.decodeString()
            return ItemPlatform.valueOf(str)
        }
        val index = decoder.decodeInt()
        return ItemPlatform.values()[index]
    }

    override fun serialize(encoder: Encoder, value: ItemPlatform) {
        if (encoder is JsonOutput) {
            encoder.encodeString(value.name.toLowerCase())
        } else {
            encoder.encodeInt(value.ordinal)
        }
    }
}

class ItemContextSerializer : KSerializer<ItemContext> {

    override val descriptor: SerialDescriptor = SerialClassDescImpl("ItemContext")

    override fun deserialize(decoder: Decoder): ItemContext {
        if (decoder is JsonInput) {
            val str = decoder.decodeString()
            return ItemContext.valueOf(str)
        }
        val index = decoder.decodeInt()
        return ItemContext.values()[index]
    }

    override fun serialize(encoder: Encoder, value: ItemContext) {
        if (encoder is JsonOutput) {
            encoder.encodeString(value.name.toLowerCase())
        } else {
            encoder.encodeInt(value.ordinal)
        }
    }
}
