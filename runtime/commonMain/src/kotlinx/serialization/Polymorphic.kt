/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*
import kotlin.reflect.*

/**
 * A [SerialDescriptor] for polymorphic serialization with special kind.
 * Currently, it has no guarantees neither on its reference transparency nor its children descriptors, only on [kind].
 */
@Deprecated(
    message = "Top-level polymorphic descriptor is deprecated, use descriptor from the instance of PolymorphicSerializer or" +
            "check for descriptor kind instead", level = DeprecationLevel.WARNING
)
public val PolymorphicClassDescriptor = SerialDescriptor("kotlinx.serialization.Polymorphic", 2, PolymorphicKind.OPEN) {
    element("class", StringSerializer.descriptor)
    // TODO make a decision on this one, see contextual
    val valueDescriptor = SerialDescriptor("value", 0) {}
    element("value", valueDescriptor)
}

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
 *         RequestA::class with RequestA.serializer()
 *         RequestB::class with RequestB.serializer()
 *     }
 *     polymorphic(BaseResponse::class) {
 *         ResponseC::class with ResponseC.serializer()
 *         ResponseD::class with ResponseD.serializer()
 *     }
 * }
 * ```
 *
 * @see SerializersModule
 * @see SerializersModuleBuilder.polymorphic
 */
public class PolymorphicSerializer<T : Any>(override val baseClass: KClass<T>) : AbstractPolymorphicSerializer<T>() {
    public override val descriptor: SerialDescriptor = PolymorphicClassDescriptor
}
