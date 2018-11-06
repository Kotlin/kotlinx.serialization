package kotlinx.serialization.cbor

import kotlinx.serialization.deprecationText

@Deprecated(deprecationText, ReplaceWith("Cbor"), DeprecationLevel.WARNING)
typealias CBOR = Cbor

@Deprecated(deprecationText, ReplaceWith("CborDecodingException"), DeprecationLevel.WARNING)
typealias CBORDecodingException = CborDecodingException
