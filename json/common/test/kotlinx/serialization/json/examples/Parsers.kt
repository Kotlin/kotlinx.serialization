package kotlinx.serialization.json.examples

import kotlinx.serialization.json.*
import kotlin.reflect.KClass

object ParserRegistry {

    @PublishedApi
    internal val registry: MutableMap<KClass<*>, JsonParser<*>> = mutableMapOf()
}

abstract class JsonParser<T: Any>(typeToken: KClass<T>) {

    init {
        @Suppress("LeakingThis")
        ParserRegistry.registry[typeToken] = this
    }

    abstract fun write(value: T): JsonElement

    abstract fun read(json: JsonObject): T

    fun read(string: String): T = read(JsonTreeParser(string).readFully().jsonObject)

    protected inline fun <reified T: Any> getParser() = ParserRegistry.registry[T::class] ?: error("Parser for class ${T::class} is not found")
}
