// This file was automatically generated from json.md by Knit tool. Do not edit.
package example.exampleJson14

import kotlinx.serialization.*
import kotlinx.serialization.json.*

fun main() {
    val element = buildJsonObject {
        put("name", "kotlinx.serialization")
        putJsonObject("owner") {
            put("name", "kotlin")
        }
        putJsonArray("forks") {
            addJsonObject {
                put("votes", 42)
            }
            addJsonObject {
                put("votes", 9000)
            }
        }
    }
    println(element)
}
