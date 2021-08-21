module kotlinx.serialization.protobuf {
    requires transitive kotlin.stdlib;
    requires transitive kotlinx.serialization.core;

    exports kotlinx.serialization.protobuf;
    exports kotlinx.serialization.protobuf.schema;
}
