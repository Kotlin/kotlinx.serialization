package kotlinx.serialization.json

import kotlinx.serialization.SerializationException

sealed class JsonException(message: String) : SerializationException(message)


class JsonInvalidValueInStrictModeException(value: Any, valueDescription: String) : JsonException(
    "$value is not a valid $valueDescription as per Json spec.\n" +
            "You can disable strict mode to serialize such values"
) {
    constructor(floatValue: Float) : this(floatValue, "float")
    constructor(doubleValue: Double) : this(doubleValue, "double")
}

class JsonUnknownKeyException(key: String) : JsonException(
    "Strict Json encountered unknown key: $key\n" +
            "You can disable strict mode to skip unknown keys"
)

class JsonParsingException(position: Int, message: String) : JsonException("Invalid Json at $position: $message")

class JsonElementTypeMismatchException(key: String, expected: String) : JsonException("Element $key is not a $expected")
