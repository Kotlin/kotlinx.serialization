package kotlinx.serialization.test

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.*
import java.io.ByteArrayOutputStream

actual fun <T> Json.encodeViaStream(
    serializer: SerializationStrategy<T>,
    value: T
): String {
    val output = ByteArrayOutputStream()
    encodeToStream(serializer, value, output)
    return output.toString()
}

actual fun <T> Json.decodeViaStream(
    serializer: DeserializationStrategy<T>,
    input: String
): T = decodeFromStream(serializer, input.byteInputStream())
