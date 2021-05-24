/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*
import kotlin.reflect.*

/**
 * This class provides support for multiplatform polymorphic serialization for interfaces and abstract classes.
 *
 * To avoid the most common security pitfalls and reflective lookup (and potential load) of an arbitrary class,
 * all serializable implementations of any polymorphic type must be [registered][SerializersModuleBuilder.polymorphic]
 * in advance in the scope of base polymorphic type, efficiently preventing unbounded polymorphic serialization
 * of an arbitrary type.
 *
 * Polymorphic serialization is enabled automatically by default for interfaces and [Serializable] abstract classes.
 * To enable this feature explicitly on other types, use `@SerializableWith(PolymorphicSerializer::class)`
 * or [Polymorphic] annotation on the property.
 *
 * Usage of the polymorphic serialization can be demonstrated by the following example:
 * ```
 * abstract class BaseRequest()
 * @Serializable
 * data class RequestA(val id: Int): BaseRequest()
 * @Serializable
 * data class RequestB(val s: String): BaseRequest()
 *
 * abstract class BaseResponse()
 * @Serializable
 * data class ResponseC(val payload: Long): BaseResponse()
 * @Serializable
 * data class ResponseD(val payload: ByteArray): BaseResponse()
 *
 * @Serializable
 * data class Message(
 *     @Polymorphic val request: BaseRequest,
 *     @Polymorphic val response: BaseResponse
 * )
 * ```
 * In this example, both request and response in `Message` are serializable with [PolymorphicSerializer].
 *
 * `BaseRequest` and `BaseResponse` are base classes and they are captured during compile time by the plugin.
 * Yet [PolymorphicSerializer] for `BaseRequest` should only allow `RequestA` and `RequestB` serializers, and none of the response's serializers.
 *
 * This is achieved via special registration function in the module:
 * ```
 * val requestAndResponseModule = SerializersModule {
 *     polymorphic(BaseRequest::class) {
 *         subclass(RequestA::class)
 *         subclass(RequestB::class)
 *     }
 *     polymorphic(BaseResponse::class) {
 *         subclass(ResponseC::class)
 *         subclass(ResponseD::class)
 *     }
 * }
 * ```
 *
 * @see SerializersModule
 * @see SerializersModuleBuilder.polymorphic
 */
@OptIn(ExperimentalSerializationApi::class)
public class PolymorphicSerializer<T : Any>(override val baseClass: KClass<T>) : AbstractPolymorphicSerializer<T>() {

    @PublishedApi // should we allow user access to this constructor?
    internal constructor(
        baseClass: KClass<T>,
        classAnnotations: Array<Annotation>
    ) : this(baseClass) {
        _descriptor.annotations = classAnnotations.asList()
    }

    private val _descriptor: SerialDescriptorImpl =
        buildSerialDescriptor("kotlinx.serialization.Polymorphic", PolymorphicKind.OPEN) {
            element("type", String.serializer().descriptor)
            element(
                "value",
                buildSerialDescriptor(
                    "kotlinx.serialization.Polymorphic<${baseClass.simpleName}>",
                    SerialKind.CONTEXTUAL
                )
            )
        } as SerialDescriptorImpl

    public override val descriptor: SerialDescriptor = _descriptor.withContext(baseClass)

    override fun toString(): String {
        return "kotlinx.serialization.PolymorphicSerializer(baseClass: $baseClass)"
    }
}

@InternalSerializationApi
public fun <T : Any> AbstractPolymorphicSerializer<T>.findPolymorphicSerializer(
    decoder: CompositeDecoder,
    klassName: String?
): DeserializationStrategy<out T> =
    findPolymorphicSerializerOrNull(decoder, klassName) ?: throwSubtypeNotRegistered(klassName, baseClass)

@InternalSerializationApi
public fun <T : Any> AbstractPolymorphicSerializer<T>.findPolymorphicSerializer(
    encoder: Encoder,
    value: T
): SerializationStrategy<T> =
    findPolymorphicSerializerOrNull(encoder, value) ?: throwSubtypeNotRegistered(value::class, baseClass)
