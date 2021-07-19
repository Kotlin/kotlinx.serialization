package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlin.test.Test
import kotlin.test.assertEquals

class EncodeDefaultModeTest {
    private class EncodedFieldDetector(private val sb: StringBuilder, private val encodeDefaults: Boolean = false) : AbstractEncoder() {
        override val serializersModule: SerializersModule = EmptySerializersModule
        private var first = false
        override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
            first = true
            sb.append('{')
            return this
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            first = false
            sb.append('}')
        }

        @ExperimentalSerializationApi
        override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean = encodeDefaults
        override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
            if (first) {
                first = false
            } else {
                sb.append(", ")
            }
            sb.append(descriptor.getElementName(index))
            return true
        }

        override fun encodeNull() {}
        override fun encodeValue(value: Any) {}
        override fun encodeString(value: String) {}
        override fun encodeChar(value: Char) {}

        companion object {
            fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T, encodeDefaults: Boolean = false): String {
                val sb = StringBuilder()
                val out = EncodedFieldDetector(sb, encodeDefaults)
                out.encodeSerializableValue(serializer, value)
                return sb.toString()
            }
        }
    }

    @Serializable
    private data class TestModel(
            @EncodeDefault(EncodeDefaultMode.DEFAULT)
            val default: Int = 0,
            @EncodeDefault(EncodeDefaultMode.ALWAYS)
            val always: Int = 1,
            @EncodeDefault(EncodeDefaultMode.NEVER)
            val never: Int = 2
    )

    @Test
    fun testEncodeDefaultMode() {
        assertEquals("{default, always}",
                EncodedFieldDetector.encodeToString(TestModel.serializer(), TestModel(), encodeDefaults = true))
        assertEquals("{always}",
                EncodedFieldDetector.encodeToString(TestModel.serializer(), TestModel(), encodeDefaults = false))
        assertEquals("{always, never}",
                EncodedFieldDetector.encodeToString(TestModel.serializer(), TestModel(0, 2, 3), encodeDefaults = false))
        assertEquals("{default, always, never}",
                EncodedFieldDetector.encodeToString(TestModel.serializer(), TestModel(0, 2, 3), encodeDefaults = true))
    }
}