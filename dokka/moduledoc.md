# Module kotlinx-serialization-core
Core serialization API and serializers for standard library classes, and ready to use JSON
format implementation.

# Module kotlinx-serialization-json
Stable and ready to use JSON format implementation, `JsonElement` API to operate with JSON trees and JSON-specific serializers.

# Module kotlinx-serialization-json-okio
Extensions for kotlinx.serialization.json.Json for integration with the popular [Okio](https://square.github.io/okio/) library.
Currently experimental.

# Module kotlinx-serialization-cbor
Concise Binary Object Representation (CBOR) format implementation, as per [RFC 7049](https://tools.ietf.org/html/rfc7049).

# Module kotlinx-serialization-hocon
Allows deserialization of `Config` object from popular [lightbend/config](https://github.com/lightbend/config) library 
into Kotlin objects.
You can learn about "Human-Optimized Config Object Notation" or HOCON from library's [readme](https://github.com/lightbend/config#using-hocon-the-json-superset).

# Module kotlinx-serialization-properties
Allows converting arbitrary hierarchy of Kotlin classes to a flat key-value structure à la Java Properties.

# Module kotlinx-serialization-protobuf
[Protocol buffers](https://protobuf.dev/) serialization format implementation.

# Package kotlinx.serialization
Basic core concepts and annotations that set up serialization process.

# Package kotlinx.serialization.builtins
Serializers for standard Kotlin types, like Int, String, List, etc.

# Package kotlinx.serialization.descriptors
Basic concepts of serial description to programmatically describe the serial form for serializers 
in an introspectable manner.

# Package kotlinx.serialization.encoding
Basic concepts of encoding and decoding of serialized data.

# Package kotlinx.serialization.modules
Classes that provides runtime mechanisms for resolving serializers, typically used during polymorphic serialization.

# Package kotlinx.serialization.hocon
HOCON serialization format implementation for converting Kotlin classes from and to [Lightbend config](https://github.com/lightbend/config).

# Package kotlinx.serialization.json
JSON serialization format implementation, JSON tree data structures with builders for them,
and JSON-specific serializers.

# Package kotlinx.serialization.json.okio
Extensions for kotlinx.serialization.json.Json for integration with the popular [Okio](https://square.github.io/okio/) library.

# Package kotlinx.serialization.protobuf
[Protocol buffers](https://protobuf.dev/) serialization format implementation.

# Package kotlinx.serialization.protobuf.schema
Experimental generator of ProtoBuf schema from Kotlin classes.

# Package kotlinx.serialization.properties
Properties serialization format implementation that represents the input data as a plain map of properties.

# Package kotlinx.serialization.cbor
Concise Binary Object Representation (CBOR) format implementation, as per [RFC 7049](https://tools.ietf.org/html/rfc7049).
