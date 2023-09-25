package kotlinx.serialization.cbor

import kotlinx.serialization.*

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
@ExperimentalSerializationApi
public annotation class CborLabel(val label: Long)