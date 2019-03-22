package kotlinx.serialization

import kotlinx.serialization.modules.getContextualOrDefault
import kotlinx.serialization.internal.HexConverter


/**
 * This annotation marks declaration which try to obtain serializer implicitly
 * using reflection, e.g. from KClass or instance itself.
 *
 * This approach is discouraged in general because it has several drawbacks, including:
 * - Reflection is not available on Kotlin/Native and is very limited on Kotlin/JS.
 * - Reflection won't infer correct serializers for generic classes, like collections.
 * - SerialModule may not be available, since it is bound to particular format, not serializer.
 * - Such reflection calls are usually slow.
 *
 * It's always better to specify serializer explicitly, using generated `.serializer()`
 * function on serializable class' companion.
 */
@Experimental
annotation class ImplicitReflectionSerializer

@ImplicitReflectionSerializer
inline fun <reified T : Any> BinaryFormat.dump(obj: T): ByteArray = dump(context.getContextualOrDefault(T::class), obj)
@ImplicitReflectionSerializer
inline fun <reified T : Any> BinaryFormat.dumps(obj: T): String = HexConverter.printHexBinary(dump(obj), lowerCase = true)

@ImplicitReflectionSerializer
inline fun <reified T : Any> BinaryFormat.load(raw: ByteArray): T = load(context.getContextualOrDefault(T::class), raw)
@ImplicitReflectionSerializer
inline fun <reified T : Any> BinaryFormat.loads(hex: String): T = load(HexConverter.parseHexBinary(hex))


@ImplicitReflectionSerializer
inline fun <reified T : Any> StringFormat.stringify(obj: T): String = stringify(context.getContextualOrDefault(T::class), obj)
@ImplicitReflectionSerializer
inline fun <reified T : Any> StringFormat.stringify(objects: List<T>): String = stringify(context.getContextualOrDefault(T::class).list, objects)
@ImplicitReflectionSerializer
inline fun <reified K : Any, reified V: Any> StringFormat.stringify(map: Map<K, V>): String
        = stringify((context.getContextualOrDefault(K::class) to context.getContextualOrDefault(V::class)).map, map)

@ImplicitReflectionSerializer
inline fun <reified T : Any> StringFormat.parse(str: String): T = parse(context.getContextualOrDefault(T::class), str)
@ImplicitReflectionSerializer
inline fun <reified T : Any> StringFormat.parseList(objects: String): List<T> = parse(context.getContextualOrDefault(T::class).list, objects)
@ImplicitReflectionSerializer
inline fun <reified K : Any, reified V: Any> StringFormat.parseMap(map: String)
        = parse((context.getContextualOrDefault(K::class) to context.getContextualOrDefault(V::class)).map, map)
