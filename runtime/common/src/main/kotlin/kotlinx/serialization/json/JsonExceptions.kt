package kotlinx.serialization.json

import kotlinx.serialization.SerializationException

@Deprecated("Renamed to JsonException", ReplaceWith("JsonException"), DeprecationLevel.HIDDEN)
typealias JSONException = JsonException

sealed class JsonException(message: String) : SerializationException(message)


@Deprecated("Renamed to JsonInvalidValueInStrictModeException", ReplaceWith("JsonInvalidValueInStrictModeException"), DeprecationLevel.HIDDEN)
typealias JSONInvalidValueInStrictModeException = JsonInvalidValueInStrictModeException

class JsonInvalidValueInStrictModeException(value: Any, valueDescription: String) : JsonException(
    "$value is not a valid $valueDescription as per Json spec.\n" +
            "You can disable strict mode to serialize such values"
) {
    constructor(floatValue: Float) : this(floatValue, "float")
    constructor(doubleValue: Double) : this(doubleValue, "double")
}


@Deprecated("Renamed to JsonUnknownKeyException", ReplaceWith("JsonUnknownKeyException"), DeprecationLevel.HIDDEN)
typealias JSONUnknownKeyException = JsonUnknownKeyException

class JsonUnknownKeyException(key: String) : JsonException(
    "Strict Json encountered unknown key: $key\n" +
            "You can disable strict mode to skip unknown keys"
)


@Deprecated("Renamed to JsonParsingException", ReplaceWith("JsonParsingException"), DeprecationLevel.HIDDEN)
typealias JSONParsingException = JsonParsingException

class JsonParsingException(position: Int, message: String) : JsonException("Invalid Json at $position: $message")


@Deprecated("Renamed to JsonElementTypeMismatchException", ReplaceWith("JsonElementTypeMismatchException"), DeprecationLevel.HIDDEN)
typealias JSONElementTypeMismatchException = JsonElementTypeMismatchException

class JsonElementTypeMismatchException(key: String, expected: String) : JsonException("Element $key is not a $expected")
