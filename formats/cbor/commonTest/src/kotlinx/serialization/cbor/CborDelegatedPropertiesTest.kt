/*
 * Part of the kotlinx.serialization test suite.
 *
 * Copyright 2025 Bernd Prünster (A-SIT Plus GmbH).
 * Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class, kotlin.ExperimentalUnsignedTypes::class)

package kotlinx.serialization.cbor

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CborDelegatedPropertiesTest {

    private companion object {
        private const val NAME_LABEL: Long = 1
        private const val AGE_LABEL: Long = 2
        private const val GENDER_LABEL: Long = 3

        private val NAME_SPEC = FieldSpec(name = "name", label = NAME_LABEL)
        private val AGE_SPEC = FieldSpec(name = "age", label = AGE_LABEL)
        private val GENDER_SPEC = FieldSpec(name = "gender", label = GENDER_LABEL)
    }

    /**
     * Showcases Kotlin delegated properties backed by a map:
     * https://kotlinlang.org/docs/delegated-properties.html#storing-properties-in-a-map
     *
     * kotlinx.serialization does not support such delegated properties out of the box, so this test provides
     * a CBOR-specific serializer that encodes/decodes the underlying map.
     */
    @Serializable(with = MapBackedPersonSerializer::class)
    private class MapBackedPerson private constructor(
        private val content: MutableMap<CborElement, CborElement>,
        public val backing: CborMap = CborMap(content),
    ) {

        var name: String by RequiredCborString(content, NAME_SPEC)
        var age: Int by RequiredCborInt(content, AGE_SPEC)

        constructor(name: String, age: Int) : this(content = mutableMapOf()) {
            this.name = name
            this.age = age
        }

        fun put(
            key: String,
            value: CborElement,
            keyTags: ULongArray = ulongArrayOf(),
            valueTags: ULongArray = ulongArrayOf(),
        ) {
            content[CborString(key, *keyTags)] = value.also { it.tags += valueTags }
        }

        fun remove(key: String, keyTags: ULongArray = ulongArrayOf()) {
            content.remove(CborString(key, *keyTags))
        }

        companion object {
            fun fromBacking(map: CborMap): MapBackedPerson {
                val content = mutableMapOf<CborElement, CborElement>()
                for ((key, value) in map.entries) {
                    val mappedKey = when (key) {
                        is CborInteger -> when {
                            key.isPositive && key.value == NAME_LABEL.toULong() -> CborString(NAME_SPEC.name, *NAME_SPEC.keyTags)
                            key.isPositive && key.value == AGE_LABEL.toULong() -> CborString(AGE_SPEC.name, *AGE_SPEC.keyTags)
                            key.isPositive && key.value == GENDER_LABEL.toULong() -> CborString(GENDER_SPEC.name, *GENDER_SPEC.keyTags)
                            else -> key
                        }

                        else -> key
                    }
                    content[mappedKey] = value
                }
                return MapBackedPerson(content, backing = CborMap(content, *map.tags))
            }
        }

        override fun equals(other: Any?): Boolean =
            other is MapBackedPerson && backing == other.backing

        override fun hashCode(): Int = backing.hashCode()

        override fun toString(): String = "MapBackedPerson(backing=$backing)"
    }

    private class RequiredCborString(
        private val content: MutableMap<CborElement, CborElement>,
        private val spec: FieldSpec,
    ) : ReadWriteProperty<Any?, String> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): String {
            val element = content[CborString(spec.name, *spec.keyTags)]
                ?: throw SerializationException("Missing required '${spec.name}' property")
            return (element as? CborString)?.value
                ?: throw SerializationException("Expected '${spec.name}' to be CborString, got ${element::class}")
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
            content[CborString(spec.name, *spec.keyTags)] = CborString(value, *spec.valueTags)
        }
    }

    private class RequiredCborInt(
        private val content: MutableMap<CborElement, CborElement>,
        private val spec: FieldSpec,
    ) : ReadWriteProperty<Any?, Int> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): Int {
            val element = content[CborString(spec.name, *spec.keyTags)]
                ?: throw SerializationException("Missing required '${spec.name}' property")
            val integer = element as? CborInteger
                ?: throw SerializationException("Expected '${spec.name}' to be CborInteger, got ${element::class}")
            return integer.intOrNull
                ?: throw SerializationException("Expected '${spec.name}' to fit into Int, got $integer")
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
            content[CborString(spec.name, *spec.keyTags)] = CborInt(value.toLong(), *spec.valueTags)
        }
    }

    private class OptionalCborString(
        private val spec: FieldSpec,
    ) : ReadWriteProperty<MapBackedPerson, String?> {
        override fun getValue(thisRef: MapBackedPerson, property: KProperty<*>): String? {
            val element = thisRef.backing[CborString(spec.name, *spec.keyTags)] ?: return null
            return (element as? CborString)?.value
                ?: throw SerializationException("Expected '${spec.name}' to be CborString, got ${element::class}")
        }

        override fun setValue(thisRef: MapBackedPerson, property: KProperty<*>, value: String?) {
            if (value == null) {
                thisRef.remove(spec.name, keyTags = spec.keyTags)
                return
            }
            thisRef.put(spec.name, CborString(value, *spec.valueTags), keyTags = spec.keyTags)
        }
    }

    private var MapBackedPerson.gender: String? by OptionalCborString(GENDER_SPEC)

    private data class FieldSpec(
        val name: String,
        val label: Long?,
        val keyTags: ULongArray = ulongArrayOf(),
        val valueTags: ULongArray = ulongArrayOf(),
    )

    private object MapBackedPersonSerializer : KSerializer<MapBackedPerson> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MapBackedPerson")

        override fun serialize(encoder: Encoder, value: MapBackedPerson) {
            val cborEncoder = encoder as? CborEncoder
                ?: throw SerializationException("MapBackedPersonSerializer is intended for CBOR tests only.")

            val preferLabels = cborEncoder.cbor.configuration.preferCborLabelsOverNames

            val out = LinkedHashMap<CborElement, CborElement>()
            for ((key, v) in value.backing.entries) {
                if (!preferLabels) {
                    out[key] = v
                    continue
                }

                val label = when (key) {
                    is CborString -> when (key.value) {
                        NAME_SPEC.name -> NAME_SPEC.label
                        AGE_SPEC.name -> AGE_SPEC.label
                        GENDER_SPEC.name -> GENDER_SPEC.label
                        else -> null
                    }

                    else -> null
                }

                out[if (label == null) key else CborInt(label)] = v
            }
            cborEncoder.encodeCborElement(CborMap(out, *value.backing.tags))
        }

        override fun deserialize(decoder: Decoder): MapBackedPerson {
            val cborDecoder = decoder as? CborDecoder
                ?: throw SerializationException("MapBackedPersonSerializer is intended for CBOR tests only.")

            val element = cborDecoder.decodeCborElement()
            val map = element as? CborMap
                ?: throw SerializationException("Expected CborMap, got ${element::class}")
            return MapBackedPerson.fromBacking(map)
        }
    }

    @Test
    fun testRoundTripToBytes() {
        val cbor = Cbor {}
        val value = MapBackedPerson("Ada", 42).also { person ->
            person.gender = "female"
            person.put("country", CborString("AT"))
        }

        val bytes = cbor.encodeToByteArray(MapBackedPersonSerializer, value)
        val decoded = cbor.decodeFromByteArray(MapBackedPersonSerializer, bytes)
        assertEquals(value, decoded)
        assertEquals("Ada", decoded.name)
        assertEquals(42, decoded.age)
        assertEquals("female", decoded.gender)
        assertEquals(CborString("AT"), decoded.backing["country"])
    }

    @Test
    fun testRoundTripViaCborElement() {
        val cbor = Cbor {}
        val value = MapBackedPerson("Ada", 42).also { person ->
            person.gender = "female"
            person.put("country", CborString("AT"))
        }
        val element = cbor.encodeToCborElement(MapBackedPersonSerializer, value)
        assertTrue(element is CborMap)
        val decoded = cbor.decodeFromCborElement(MapBackedPersonSerializer, element)
        assertEquals(value, decoded)
        assertEquals("female", decoded.gender)
        assertEquals(CborString("AT"), decoded.backing["country"])
    }

    @Test
    fun testPreferLabelsOverNamesAffectsEncoding() {
        val cbor = Cbor { preferCborLabelsOverNames = true }
        val value = MapBackedPerson("Ada", 42).also { person ->
            person.gender = "female"
            person.put("country", CborString("AT"))
        }

        val element = cbor.encodeToCborElement(MapBackedPersonSerializer, value)
        assertTrue(element is CborMap)
        assertEquals(CborString("Ada"), element.getValue(NAME_LABEL))
        assertEquals(CborInt(42), element.getValue(AGE_LABEL))
        assertEquals(CborString("female"), element.getValue(GENDER_LABEL))
        assertEquals(CborString("AT"), element.getValue("country"))

        // Decoder accepts both string keys and label keys, but normalizes back to string keys in the backing map.
        val decoded = cbor.decodeFromCborElement(MapBackedPersonSerializer, element)
        assertEquals("Ada", decoded.name)
        assertEquals(42, decoded.age)
        assertEquals("female", decoded.gender)
        assertEquals(CborString("AT"), decoded.backing["country"])
        assertTrue(decoded.backing["name"] is CborString)
    }
}
