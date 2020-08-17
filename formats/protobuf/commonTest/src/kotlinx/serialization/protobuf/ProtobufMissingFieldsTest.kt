/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*
import kotlin.test.*

class ProtobufMissingFieldsTest {

    private val buffer = byteArrayOf(10, 30, 8, 11, 16, 2, 26, 3, 115, 112, 97, 26, 6, 115, 112, 97, 95, 101, 115, 26, 2, 101, 115, 26, 5, 101, 115, 95, 101, 115, 32, 1, 16, 25)

    @Test
    fun testDeserializeAllFields() {
        val items = ProtoBuf.decodeFromByteArray(Items.serializer(), buffer)
        assertEquals(25, items.pageSize)
        assertFalse(items.nextPage)
        assertEquals(1, items.items.size)
        assertEquals(ItemPlatform.Android, items.items[0].platform)
        assertEquals(11, items.items[0].id)
        assertEquals(listOf("spa", "spa_es", "es", "es_es"), items.items[0].language)
        assertEquals(ItemContext.Context1, items.items[0].context)
    }

    @Test
    fun testDeserializeSomeTagsAreNotInSchema() {
        val items = ProtoBuf.decodeFromByteArray(ItemsWithoutPageSize.serializer(), buffer)
        assertFalse(items.nextPage)
        assertEquals(1, items.items.size)
        assertEquals(11, items.items[0].id)
        assertEquals(listOf("spa", "spa_es", "es", "es_es"), items.items[0].language)
        assertEquals(ItemContext.Context1, items.items[0].context)
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
        @ProtoNumber(1)
        val items: List<Item> = emptyList(),
        @ProtoNumber(2)
        val pageSize: Int? = null,
        @ProtoNumber(3)
        val nextPage: Boolean = false
    )

    @Serializable
    data class Item(
        @ProtoNumber(1)
        val id: Int,
        @ProtoNumber(2) @Serializable(with = ItemPlatformSerializer::class)
        val platform: ItemPlatform = ItemPlatform.Unknown,
        @ProtoNumber(3)
        val language: List<String> = emptyList(),
        @ProtoNumber(4) @Serializable(with = ItemContextSerializer::class)
        val context: ItemContext = ItemContext.Unknown
    )

    @Serializable
    data class ItemsWithoutPageSize(
        @ProtoNumber(1)
        val items: List<ItemWithoutPlatform> = emptyList(),
        @ProtoNumber(3)
        val nextPage: Boolean = false
    )

    @Serializable
    data class ItemWithoutPlatform(
        @ProtoNumber(1)
        val id: Int,
        @ProtoNumber(3)
        val language: List<String> = emptyList(),
        @ProtoNumber(4) @Serializable(with = ItemContextSerializer::class)
        val context: ItemContext = ItemContext.Unknown
    )

    class ItemPlatformSerializer : KSerializer<ItemPlatform> {
        override val descriptor: SerialDescriptor = buildSerialDescriptor("ItemPlatform", SerialKind.ENUM) {
            enumValues<ItemPlatform>().forEach {
                element(it.name, buildSerialDescriptor("$serialName.${it.name}", StructureKind.OBJECT))
            }
        }

        override fun deserialize(decoder: Decoder): ItemPlatform {
            val index = decoder.decodeInt()
            return ItemPlatform.values()[index]
        }

        override fun serialize(encoder: Encoder, value: ItemPlatform) {
            encoder.encodeInt(value.ordinal)
        }
    }

    class ItemContextSerializer : KSerializer<ItemContext> {

        override val descriptor: SerialDescriptor = buildSerialDescriptor("ItemContext", SerialKind.ENUM) {
            enumValues<ItemContext>().forEach {
                element(it.name, buildSerialDescriptor("$serialName.${it.name}", StructureKind.OBJECT))
            }
        }

        override fun deserialize(decoder: Decoder): ItemContext {
            val index = decoder.decodeInt()
            return ItemContext.values()[index]
        }

        override fun serialize(encoder: Encoder, value: ItemContext) {
            encoder.encodeInt(value.ordinal)
        }
    }
}
