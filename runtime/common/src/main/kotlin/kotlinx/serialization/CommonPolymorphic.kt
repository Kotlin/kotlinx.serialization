package kotlinx.serialization

import kotlinx.serialization.internal.SerialClassDescImpl
import kotlinx.serialization.context.MutableSerialContext
import kotlin.reflect.KClass

/**
 * A [SerialDescriptor] for polymorphic serialization with special kind.
 * Currently bo guarantees on its semantics except referential equality.
 */
object PolymorphicClassDescriptor : SerialClassDescImpl("kotlin.Any") {
    override val kind: SerialKind = UnionKind.POLYMORPHIC

    init {
        // todo: serial ids for this?
        addElement("class")
        addElement("value")
    }
}

/**
 * Thrown when a subclass was not registered for polymorphic serialization in a scope of given `basePolyType`.
 *
 * @see MutableSerialContext.registerPolymorphicSerializer
 */
class SubtypeNotRegisteredException(subClassName: String, basePolyType: KClass<*>):
    SerializationException("$subClassName is not registered for polymorphic serialization in the scope of $basePolyType") {

    constructor(subClass: KClass<*>, basePolyType: KClass<*>): this(subClass.toString(), basePolyType)
}


/**
 * This class provides support for multiplatform polymorphic serialization.
 * Due to security and reflection usage concerns, all serializable implementations of some abstract class must be registered in advance.
 * However, it allows registering subclasses in runtime, not compile-time. For example, it allows adding additional subclasses to the registry
 * that were defined in a separate module, dependent on the base module with the base class.
 *
 * Polymorphic serialization is never enabled automatically. To enable this feature, use @SerializableWith(PolymorphicSerializer::class) or just @Polymorphic on the property.
 *
 * Another security requirement is that we only allow registering subclasses in the scope of a base class called [basePolyType]
 * The motivation for this is easily understandable from the example:

```
abstract class BaseRequest()
@Serializable data class RequestA(val id: Int): BaseRequest()
@Serializable data class RequestB(val s: String): BaseRequest()

abstract class BaseResponse()
@Serializable data class ResponseC(val payload: Long): BaseResponse()
@Serializable data class ResponseD(val payload: ByteArray): BaseResponse()

@Serializable data class Message(
    @Polymorphic val request: BaseRequest,
    @Polymorphic val response: BaseResponse
)
```
 * In this example, both request and response in Message are serializable with [PolymorphicSerializer] because of the annotation on them;
 * BaseRequest and BaseResponse became [basePolyType]s as they're captured during compile time by the plugin. They are not required to be serializable by themselves.
 * Yet PolymorphicSerializer for request should only allow RequestA and RequestB serializers, and none of the response's serializers.
 * This is achieved via [MutableSerialContext.registerPolymorphicSerializer] function, which accepts two KClass references.
 *
 * By default (without special support from [Encoder]), polymorphic values are serialized as list with
 * two elements: classname (String) and the object itself.
 *
 * @see MutableSerialContext.registerPolymorphicSerializer
 */
@Suppress("UNCHECKED_CAST")
class PolymorphicSerializer<T : Any>(private val basePolyType: KClass<T>) : KSerializer<Any> {

    override val descriptor: SerialDescriptor
        get() = PolymorphicClassDescriptor

    override fun serialize(encoder: Encoder, obj: Any) {
        val actualSerializer = findPolymorphicSerializer(encoder, obj)
        val out = encoder.beginStructure(descriptor)
        out.encodeStringElement(descriptor, 0, actualSerializer.descriptor.name)
        out.encodeSerializableElement(descriptor, 1, actualSerializer as KSerializer<Any>, obj)
        out.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): Any {
        @Suppress("NAME_SHADOWING")
        val input = decoder.beginStructure(descriptor)
        var klassName: String? = null
        var value: Any? = null
        mainLoop@ while (true) {
            when (val index = input.decodeElementIndex(descriptor)) {
                CompositeDecoder.READ_ALL -> {
                    klassName = input.decodeStringElement(descriptor, 0)
                    val loader = findPolymorphicSerializer(input, klassName)
                    value = input.decodeSerializableElement(descriptor, 1, loader)
                    break@mainLoop
                }
                CompositeDecoder.READ_DONE -> {
                    break@mainLoop
                }
                0 -> {
                    klassName = input.decodeStringElement(descriptor, index)
                }
                1 -> {
                    klassName = requireNotNull(klassName) { "Cannot read polymorphic value before its type token" }
                    val loader = input.context.resolveFromBase(basePolyType, klassName)
                            ?: throw SubtypeNotRegisteredException(klassName, basePolyType)
                    value = input.decodeSerializableElement(descriptor, index, loader)
                }
                else -> throw SerializationException("Invalid index in polymorphic deserialization of " +
                        "${klassName ?: "unknown class"} with base $basePolyType" +
                        "\n Expected 0, 1, READ_ALL(-2) or READ_DONE(-1), but found $index")
            }
        }

        input.endStructure(descriptor)
        return requireNotNull(value) { "Polymorphic value have not been read" }
    }

    /**
     * Lookup of the polymorphic serializer by given [fqnClassName] in the context of [decoder] by the base class [basePolyType].
     * Throws [SubtypeNotRegisteredException] if serializer is not found.
     */
    public fun findPolymorphicSerializer(decoder: CompositeDecoder, fqnClassName: String): KSerializer<T> {
        return decoder.context.resolveFromBase(basePolyType, fqnClassName) as? KSerializer<T>
            ?: throw SubtypeNotRegisteredException(fqnClassName, basePolyType)
    }

    /**
     * Lookup of the polymorphic serializer for given [value] in the context of [encoder] by the base class [basePolyType].
     * Throws [SubtypeNotRegisteredException] if serializer is not found.
     */
    public fun findPolymorphicSerializer(encoder: Encoder, value: Any): KSerializer<T> {
        return (encoder.context.resolveFromBase(basePolyType, value as T) as? KSerializer<T>
            ?: throw SubtypeNotRegisteredException(value::class, basePolyType))
    }
}
