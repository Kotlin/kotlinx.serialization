package kotlinx.serialization.cbor

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy

public interface CborSerializationStrategy<in T>: SerializationStrategy<T> {
    public fun serialize(encoder: CborEncoder, value: T)
}

public interface CborDeserializationStrategy<T>: DeserializationStrategy<T> {
    public fun deserialize(decoder: CborDecoder): T
}

public interface CborKSerializer<T>: KSerializer<T>, CborSerializationStrategy<T>, CborDeserializationStrategy<T>
