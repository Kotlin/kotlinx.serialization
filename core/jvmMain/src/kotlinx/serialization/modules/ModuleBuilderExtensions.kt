package kotlinx.serialization.modules

import kotlinx.serialization.*
import kotlin.reflect.*
import kotlin.reflect.full.*


/**
 * Accept a serializer, associated with [actualClass] for polymorphic serialization.
 *
 * Use this function only if the generic parameters of [baseClass] are erased,
 * check the inheritance at runtime.
 *
 * Otherwise, use [polymorphic].
 *
 * If [ignoreIncorrectInheritance] is false, an [IncorrectInheritanceException] throws.
 */
@ExperimentalSerializationApi
public fun SerializersModuleBuilder.polymorphicUnsafely(
    baseClass: KClass<*>,
    actualClass: KClass<*>,
    actualSerializer: KSerializer<*>,
    allowOverwrite: Boolean = false,
    ignoreIncorrectInheritance: Boolean = false
) {
    if (!actualClass.isSubclassOf(baseClass)) {
        if (ignoreIncorrectInheritance) return
        throw IncorrectInheritanceException(baseClass, actualClass)
    }
    this.registerPolymorphicSerializer(baseClass, actualClass, actualSerializer, allowOverwrite)
}

/**
 * Accept [actualClass], associated with it and its superClasses for polymorphic serialization.
 */
@ExperimentalSerializationApi
public fun SerializersModuleBuilder.polymorphicAllSuperClasses(
    actualClass: KClass<*>,
    allowOverwrite: Boolean = false
) {
    actualClass.allSuperclasses.forEach { superClass ->
        polymorphicUnsafely(superClass, actualClass, actualClass.serializer(), allowOverwrite)
    }
}

/**
 * Accept [actualClass], associated with it and its superClasses for polymorphic serialization.
 * Recursively do same thing on all super classes of [actualClass]
 */
@ExperimentalSerializationApi
public fun SerializersModuleBuilder.polymorphicSuperRecursive(actualClass: KClass<*>) {
    actualClass.allSuperclasses.forEach { superClass ->
        polymorphicSuperRecursive(superClass)
        polymorphicUnsafely(superClass, actualClass, actualClass.serializer(), true)
    }
}

