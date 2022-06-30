package kotlinx.serialization.test

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json

public expect fun <T> Json.encodeViaStream(serializer: SerializationStrategy<T>, value: T): String

public expect fun <T> Json.decodeViaStream(serializer: DeserializationStrategy<T>, input: String): T
