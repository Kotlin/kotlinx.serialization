package kotlinx.serialization.test

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json

actual fun <T> Json.encodeViaStream(
    serializer: SerializationStrategy<T>,
    value: T
): String {
    TODO("supported on JVM only")
}

actual fun <T> Json.decodeViaStream(
    serializer: DeserializationStrategy<T>,
    input: String
): T {
    TODO("supported on JVM only")
}