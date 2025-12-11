// This file was automatically generated from polymorphism.md by Knit tool. Do not edit.
package example.examplePoly11

import kotlinx.serialization.*
import kotlinx.serialization.json.*

import kotlinx.serialization.modules.*

interface Base

@Serializable
sealed interface Sub: Base

@Serializable
class Sub1(val data: String): Sub

val module1 = SerializersModule {
  polymorphic(Base::class) {
     subclassesOfSealed(Sub.serializer())
  }
}

val format1 = Json { serializersModule = module1 }

val module2 = SerializersModule {
  polymorphic(Base::class) {
     subclassesOfSealed<Sub>()
  }
}

val format2 = Json { serializersModule = module2 }


fun main() {
    val data: Base = Sub1("kotlin")
    println(format1.encodeToString(data))
    println(format2.encodeToString(data))
}
