// This file was automatically generated from formats.md by Knit tool. Do not edit.
package example.exampleFormats04

import kotlinx.serialization.*
import kotlinx.serialization.cbor.*

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val element: CborElement = Cbor.decodeFromHexString("a165627974657343666f6f")
    println(element)
}
