package kotlinx.serialization

import kotlinx.serialization.modules.getContextualOrDefault
import kotlinx.serialization.internal.InternalHexConverter
import kotlinx.serialization.json.*

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
public annotation class ImplicitReflectionSerializer

/**
 * This annotation marks declarations with default parameters that are subject to semantic change without a migration path.
 *
 * For example, [JsonConfiguration.Default] marked as unstable, thus indicating that it can change its default values
 * in the next releases (e.g. disable strict-mode by default), leading to a semantic (not source code level) change.
 */
@Experimental(level = Experimental.Level.WARNING)
public annotation class UnstableDefault

@ImplicitReflectionSerializer
public inline fun <reified T : Any> BinaryFormat.dump(obj: T): ByteArray = dump(context.getContextualOrDefault(T::class), obj)
@ImplicitReflectionSerializer
public inline fun <reified T : Any> BinaryFormat.dumps(obj: T): String = InternalHexConverter.printHexBinary(dump(obj), lowerCase = true)

@ImplicitReflectionSerializer
public inline fun <reified T : Any> BinaryFormat.load(raw: ByteArray): T = load(context.getContextualOrDefault(T::class), raw)
@ImplicitReflectionSerializer
public inline fun <reified T : Any> BinaryFormat.loads(hex: String): T = load(InternalHexConverter.parseHexBinary(hex))


@ImplicitReflectionSerializer
public inline fun <reified T : Any> StringFormat.stringify(obj: T): String = stringify(context.getContextualOrDefault(T::class), obj)
@ImplicitReflectionSerializer
public inline fun <reified T : Any> StringFormat.stringify(objects: List<T>): String = stringify(context.getContextualOrDefault(T::class).list, objects)
@ImplicitReflectionSerializer
public inline fun <reified K : Any, reified V: Any> StringFormat.stringify(map: Map<K, V>): String
        = stringify((context.getContextualOrDefault(K::class) to context.getContextualOrDefault(V::class)).map, map)

@ImplicitReflectionSerializer
public inline fun <reified T : Any> StringFormat.parse(str: String): T = parse(context.getContextualOrDefault(T::class), str)
@ImplicitReflectionSerializer
public inline fun <reified T : Any> StringFormat.parseList(objects: String): List<T> = parse(context.getContextualOrDefault(T::class).list, objects)
@ImplicitReflectionSerializer
public inline fun <reified K : Any, reified V: Any> StringFormat.parseMap(map: String)
        = parse((context.getContextualOrDefault(K::class) to context.getContextualOrDefault(V::class)).map, map)
