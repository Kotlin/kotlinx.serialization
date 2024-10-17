// This file was automatically generated from serialization-json-elements.md by Knit tool. Do not edit.
package example.exampleJsonElements03

import kotlinx.serialization.*
import kotlinx.serialization.json.*

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

fun main() {
    val element = buildJsonObject {
        // Adds a simple key-value pair to the JsonObject
        put("name", "kotlinx.serialization")
        // Adds a nested JsonObject under the "owner" key
        putJsonObject("owner") {
            put("name", "kotlin")
        }
        // Adds a JsonArray with multiple JsonObjects
        putJsonArray("forks") {
            // Adds a JsonObject to the JsonArray
            addJsonObject {
                put("votes", 42)
            }
            addJsonObject {
                put("votes", 9000)
            }
        }
    }
    // Prints the resulting JSON string
    println(element)
    // {"name":"kotlinx.serialization","owner":{"name":"kotlin"},"forks":[{"votes":42},{"votes":9000}]}
}
