package kotlinx.serialization

import kotlinx.serialization.internal.*
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.companionObjectInstance
import kotlin.reflect.full.isSubclassOf

/**
 *  @author Leonid Startsev
 *          sandwwraith@gmail.com
 */


@Suppress("UNCHECKED_CAST", "DEPRECATION")
fun <T : Any> KClass<T>.serializer(): KSerializer<T> = companionObjectInstance as KSerializer<T>

// for user-defined external serializers
fun registerSerializer(forClassName: String, serializer: KSerializer<*>) {
    SerialCache.map.put(forClassName, serializer)
}

fun <E> serializerByValue(value: E): KSerializer<E> {
    val klass = (value as? Any)?.javaClass?.kotlin ?: throw SerializationException("Cannot determine class for value $value")
    return serializerByClass(klass)
}

fun <E> serializerByClass(className: String): KSerializer<E> = SerialCache.lookupSerializer(className)

fun <E> serializerByClass(klass: KClass<*>): KSerializer<E> = SerialCache.lookupSerializer(klass.qualifiedName!!, klass)

fun serializerByTypeToken(type: Type): KSerializer<Any> = when(type) {
    is Class<*> -> serializerByClass(type.kotlin)
    is ParameterizedType -> {
        val rootClass = (type.rawType as Class<*>).kotlin
        val args = (type.actualTypeArguments)
        @Suppress("UNCHECKED_CAST")
        when {
            rootClass.isSubclassOf(List::class) -> ArrayListSerializer(serializerByTypeToken(args[0])) as KSerializer<Any>
            rootClass.isSubclassOf(Set::class) -> HashSetSerializer(serializerByTypeToken(args[0])) as KSerializer<Any>
            rootClass.isSubclassOf(Map::class) -> HashMapSerializer(serializerByTypeToken(args[0]), serializerByTypeToken(args[1])) as KSerializer<Any>
            rootClass.isSubclassOf(Map.Entry::class) -> MapEntrySerializer(serializerByTypeToken(args[0]), serializerByTypeToken(args[1])) as KSerializer<Any>
            else -> serializerByClass(rootClass)
        }
    }
    else -> throw IllegalArgumentException()
}