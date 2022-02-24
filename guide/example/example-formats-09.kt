// This file was automatically generated from formats.md by Knit tool. Do not edit.
package example.exampleFormats09

import kotlinx.serialization.*
import kotlinx.serialization.properties.Properties // todo: remove when no longer needed
import kotlinx.serialization.properties.*

@Serializable
class Project(val name: String, val owner: User)

@Serializable
class User(val name: String)

fun main() {
    val data = Project("kotlinx.serialization",  User("kotlin"))
    val map = Properties.encodeToMap(data)
    map.forEach { (k, v) -> println("$k = $v") }
}
