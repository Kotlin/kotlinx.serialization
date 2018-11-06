package kotlinx.serialization.cbor

import kotlinx.serialization.deprecationText

@Deprecated(deprecationText, ReplaceWith("Cbor"), DeprecationLevel.WARNING)
typealias CBOR = Cbor

@Deprecated(deprecationText, ReplaceWith("CborDecodingException"), DeprecationLevel.WARNING)
typealias CBORDecodingException = CborDecodingException

// TODO Nested and local type aliases are not supported
//@Deprecated(deprecationText, ReplaceWith("CborEncoder"), DeprecationLevel.WARNING)
//typealias Cbor.CBOREncoder = Cbor.CborEncoder
//
//@Deprecated(deprecationText, ReplaceWith("CborDecoder"), DeprecationLevel.WARNING)
//typealias Cbor.CBORDecoder = Cbor.CborDecoder
