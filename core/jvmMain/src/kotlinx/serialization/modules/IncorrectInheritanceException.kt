package kotlinx.serialization.modules

import kotlin.reflect.KClass

public class IncorrectInheritanceException internal constructor(msg: String) : IllegalArgumentException(msg) {
    public constructor(
        baseClass: KClass<*>,
        concreteClass: KClass<*>
    ) : this("$baseClass is not the super class of $concreteClass")
}