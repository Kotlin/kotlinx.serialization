// This file was automatically generated from alternative-serialization-formats.md by Knit tool. Do not edit.
package example.exampleFormats03

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.cbor.*

fun ByteArray.toAsciiHexString() = joinToString("") {
    if (it in 32..127) it.toInt().toChar().toString() else
        "{${it.toUByte().toString(16).padStart(2, '0').uppercase()}}"
}

@Serializable
data class Data(
    // Encodes the byte array as CBOR major type 2 as a continuous byte string
    @ByteString
    val type2: ByteArray,
    // Encodes the byte array as CBOR major type 4 as an array of individual data items
    val type4: ByteArray
)        

fun main() {
    // Creates a Data object with two ByteArray fields
    val data = Data(byteArrayOf(1, 2, 3, 4), byteArrayOf(5, 6, 7, 8))
    // Serializes the Data object into a CBOR byte array
    val bytes = Cbor.encodeToByteArray(data)
    println(bytes.toAsciiHexString())
    // {BF}etype2D{01}{02}{03}{04}etype4{9F}{05}{06}{07}{08}{FF}{FF}
    
    val obj = Cbor.decodeFromByteArray<Data>(bytes)
    println(obj)
    // Data(type2=[1, 2, 3, 4], type4=[5, 6, 7, 8])
}
