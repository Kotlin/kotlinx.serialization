/*
 * Copyright 2017-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.r8

import kotlinx.serialization.*
import kotlin.reflect.*

@Serializable
class ObfuscatedClass(val name: String) {
    fun used() {
        println("Hello $name!")
    }

    fun unused() {
        println("Hello $name!")
    }
}

@Serializable
class UnusedClass(val name: String)

@Serializable
class AccessSerializer(val name: String)

@Serializable
class SerializableSimple(val name: String)

class Container {
    @Serializable
    class SerializableNested(val name: String)
}



@Serializable
object SerializableObject

@Serializable
enum class SerializableEnum {
    A, B
}

@Serializable
class SerializableWithNamedCompanion(val i: Int) {
    companion object CustomName {
        fun method() {
            println("Hello")
        }
    }
}

@Serializable
sealed interface SealedInterface {
    val body: String
}

// and this class too has implicit @Polymorphic
@Serializable
abstract class AbstractClass : SealedInterface {
    abstract override val body: String
}

@Polymorphic
@Serializable
open class OpenPolymorphicClass : AbstractClass() {
    override var body: String = "Simple"
}

@Serializable
open class OpenClass : AbstractClass() {
    override var body: String = "Simple"
}

annotation class MyAnnotation

@MyAnnotation
object ExampleObject

val type = typeOf<ExampleObject>()