package kotlinx.serialization.cbor

import kotlinx.serialization.*

@SerialInfo
@Target(AnnotationTarget.CLASS)
@ExperimentalSerializationApi
public annotation class CborArray(@OptIn(ExperimentalUnsignedTypes::class) vararg val tag: ULong)