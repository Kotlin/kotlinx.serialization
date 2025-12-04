// This file was automatically generated from polymorphism.md by Knit tool. Do not edit.
package example.examplePoly21

import kotlinx.serialization.*
import kotlinx.serialization.json.*

import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*

interface Animal {
}

interface Cat : Animal {
    val catType: String
}

interface Dog : Animal {
    val dogType: String
}

private class CatImpl : Cat {
    override val catType: String = "Tabby"
}

private class DogImpl : Dog {
    override val dogType: String = "Husky"
}

object AnimalProvider {
    fun createCat(): Cat = CatImpl()
    fun createDog(): Dog = DogImpl()
}

val module = SerializersModule {
    polymorphicDefaultSerializer(Animal::class) { instance ->
        @Suppress("UNCHECKED_CAST")
        when (instance) {
            is Cat -> CatSerializer as SerializationStrategy<Animal>
            is Dog -> DogSerializer as SerializationStrategy<Animal>
            else -> null
        }
    }
}

object CatSerializer : SerializationStrategy<Cat> {
    override val descriptor = buildClassSerialDescriptor("Cat") {
        element<String>("catType")
    }
  
    override fun serialize(encoder: Encoder, value: Cat) {
        encoder.encodeStructure(descriptor) {
          encodeStringElement(descriptor, 0, value.catType)
        }
    }
}

object DogSerializer : SerializationStrategy<Dog> {
  override val descriptor = buildClassSerialDescriptor("Dog") {
    element<String>("dogType")
  }

  override fun serialize(encoder: Encoder, value: Dog) {
    encoder.encodeStructure(descriptor) {
      encodeStringElement(descriptor, 0, value.dogType)
    }
  }
}

val format = Json { serializersModule = module }

fun main() {
    println(format.encodeToString<Animal>(AnimalProvider.createCat()))
}
