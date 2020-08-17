# Module kotlinx-serialization-runtime

A companion library to kotlinx.serialization compiler plugin.
Contains core serialization API, serializers for standard library classes, and ready to use JSON
format implementation.
Other formats implementations (Protobuf, CBOR) are provided by separate artifacts, but included to this document for integrity.

# Package kotlinx.serialization

Basic core concepts and annotations that set up serialization process.

# Package kotlinx.serialization.builtins

Serializers for standard Kotlin types, like Int, String, List, etc.

# Package kotlinx.serialization.modules

Classes that provides runtime mechanisms for resolving serializers, typically used during polymorphic serialization.

# Package kotlinx.serialization.json

JSON serialization format implementation, JSON syntax tree data structures with builders for them,
and JSON-specific serializers.

# Package kotlinx.serialization.protobuf

Protocol buffers serialization format implementation, mostly complaint to [proto2](https://developers.google.com/protocol-buffers/docs/proto) specification. Located in separate `kotlinx-serialization-protobuf` artifact.

# Package kotlinx.serialization.cbor

Concise Binary Object Representation (CBOR) format implementation, as per [RFC 7049](https://tools.ietf.org/html/rfc7049). Located in separate `kotlinx-serialization-cbor` artifact.
