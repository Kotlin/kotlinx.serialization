package kotlinx.serialization.json

import kotlinx.serialization.KSerializer

class JsonDynamicImplicitNullsTest : AbstractJsonImplicitNullsTest() {
    override fun <T> Json.encode(value: T, serializer: KSerializer<T>): String {
        return JSON.stringify(encodeToDynamic(serializer, value))
    }

    override fun <T> Json.decode(json: String, serializer: KSerializer<T>): T {
        val x: dynamic = JSON.parse(json)
        return decodeFromDynamic(serializer, x)
    }
}
