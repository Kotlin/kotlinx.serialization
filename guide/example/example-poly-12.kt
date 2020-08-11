// This file was automatically generated from polymorphism.md by Knit tool. Do not edit.
package example.examplePoly12

import kotlinx.serialization.*
import kotlinx.serialization.json.*

import kotlinx.serialization.modules.*

val module = SerializersModule {
    polymorphic(Any::class) {
        subclass(OwnedProject::class)
    }
}
val format = Json { serializersModule = module }

@Serializable
abstract class Project {
    abstract val name: String
}
            
@Serializable
@SerialName("owned")
class OwnedProject(override val name: String, val owner: String) : Project()

fun main() {
    val data: Any = OwnedProject("kotlinx.coroutines", "kotlin")
    println(format.encodeToString(data))
}    
