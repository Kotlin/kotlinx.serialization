package kotlinx.serialization.cbor

import kotlinx.serialization.*

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
@ExperimentalSerializationApi
public annotation class SerialLabel(val label: Long)