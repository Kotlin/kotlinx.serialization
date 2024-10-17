// This file was automatically generated from alternative-serialization-formats.md by Knit tool. Do not edit.
package example.exampleFormats04

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*

fun ByteArray.toAsciiHexString() = joinToString("") {
    if (it in 32..127) it.toInt().toChar().toString() else
        "{${it.toUByte().toString(16).padStart(2, '0').uppercase()}}"
}

@Serializable
data class Project(val name: String, val language: String)

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val data = Project("kotlinx.serialization", "Kotlin")
    // Serializes the Project instance into a ProtoBuf byte array
    val bytes = ProtoBuf.encodeToByteArray(data)
    
    // Converts the byte array into a readable hex string for demonstration
    println(bytes.toAsciiHexString())
    // {0A}{15}kotlinx.serialization{12}{06}Kotlin
    
    val obj = ProtoBuf.decodeFromByteArray<Project>(bytes)
    println(obj)
    // Project(name=kotlinx.serialization, language=Kotlin)
}

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*

fun ByteArray.toAsciiHexString() = joinToString("") {
    if (it in 32..127) it.toInt().toChar().toString() else
        "{${it.toUByte().toString(16).padStart(2, '0').uppercase()}}"
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Project(
    // Assigns field number 1 to the name property
    @ProtoNumber(1)
    val name: String,
    // Assigns field number 3 to the language property
    @ProtoNumber(3)
    val language: String
)

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val data = Project("kotlinx.serialization", "Kotlin") 
    val bytes = ProtoBuf.encodeToByteArray(data)   
    println(bytes.toAsciiHexString())
    // {0A}{15}kotlinx.serialization{1A}{06}Kotlin
    val obj = ProtoBuf.decodeFromByteArray<Project>(bytes)
    println(obj)
    // Project(name=kotlinx.serialization, language=Kotlin)
}

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*

fun ByteArray.toAsciiHexString() = joinToString("") {
    if (it in 32..127) it.toInt().toChar().toString() else
        "{${it.toUByte().toString(16).padStart(2, '0').uppercase()}}"
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
class Data(
    // Uses DEFAULT encoding, optimized for small non-negative numbers
    @ProtoType(ProtoIntegerType.DEFAULT)
    val a: Int,
    // Uses SIGNED encoding, optimized for small signed integers
    @ProtoType(ProtoIntegerType.SIGNED)
    val b: Int,
    // Uses FIXED encoding, which always uses a fixed number of bytes
    @ProtoType(ProtoIntegerType.FIXED)
    val c: Int
)

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val data = Data(1, -2, 3) 
    println(ProtoBuf.encodeToByteArray(data).toAsciiHexString())
    // {08}{01}{10}{03}{1D}{03}{00}{00}{00}
}

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*

fun ByteArray.toAsciiHexString() = joinToString("") {
    if (it in 32..127) it.toInt().toChar().toString() else
        "{${it.toUByte().toString(16).padStart(2, '0').uppercase()}}"
}

@Serializable
data class Data(
    // Sets default values for the lists to ensure empty lists can be deserialized
    val a: List<Int> = emptyList(),
    val b: List<Int> = emptyList()
)

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val data = Data(listOf(1, 2, 3), listOf())
    val bytes = ProtoBuf.encodeToByteArray(data)
    println(bytes.toAsciiHexString())
    // {08}{01}{08}{02}{08}{03}
    println(ProtoBuf.decodeFromByteArray<Data>(bytes))
    // Data(a=[1, 2, 3], b=[])
}

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*

// Defines the data class with a oneof property
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Data(
    // Represents the name field with field number 1
    @ProtoNumber(1) val name: String,
    // Uses the IPhoneType interface for the oneof phone group
    @ProtoOneOf val phone: IPhoneType?,
)

// The sealed interface representing the 'oneof' group
@Serializable sealed interface IPhoneType

// Represents the home_phone field from the oneof group
@OptIn(ExperimentalSerializationApi::class)
@Serializable @JvmInline value class HomePhone(@ProtoNumber(2) val number: String): IPhoneType

// Represents the work_phone field from the oneof group
@OptIn(ExperimentalSerializationApi::class)
@Serializable data class WorkPhone(@ProtoNumber(3) val number: String): IPhoneType

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val dataTom = Data("Tom", HomePhone("123"))
    val stringTom = ProtoBuf.encodeToHexString(dataTom)
    val dataJerry = Data("Jerry", WorkPhone("789"))
    val stringJerry = ProtoBuf.encodeToHexString(dataJerry)
    println(stringTom)
    // 0a03546f6d1203313233
    println(stringJerry)
    // 0a054a657272791a03373839
    println(ProtoBuf.decodeFromHexString<Data>(stringTom))
    // Data(name=Tom, phone=HomePhone(number=123))
    println(ProtoBuf.decodeFromHexString<Data>(stringJerry))
    // Data(name=Jerry, phone=WorkPhone(number=789))
}

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*
import kotlinx.serialization.protobuf.schema.ProtoBufSchemaGenerator

@Serializable
data class SampleData(
    val amount: Long,
    val description: String?,
    val department: String = "QA"
)

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val descriptors = listOf(SampleData.serializer().descriptor)
    val schemas = ProtoBufSchemaGenerator.generateSchemaText(descriptors)
    println(schemas)
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-properties:%serializationVersion%")
}

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.*

@Serializable
class Project(val name: String, val owner: User)

@Serializable
class User(val name: String)

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val data = Project("kotlinx.serialization",  User("kotlin"))
    // Encodes the data object into a map with dot-separated keys for nested properties
    val map = Properties.encodeToMap(data)
    // Iterates through the map and prints the key-value pairs
    map.forEach { (k, v) -> println("$k = $v") }
    // name = kotlinx.serialization
    // owner.name = kotlin
}

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.descriptors.*
import kotlin.io.encoding.*

@OptIn(ExperimentalEncodingApi::class)
// Custom serializer for converting ByteArray to Base64 format and back
object ByteArrayAsBase64Serializer : KSerializer<ByteArray> {
    private val base64 = Base64.Default

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor(
            "ByteArrayAsBase64Serializer",
            PrimitiveKind.STRING
        )

    override fun serialize(encoder: Encoder, value: ByteArray) {
        val base64Encoded = base64.encode(value)
        encoder.encodeString(base64Encoded)
    }

    override fun deserialize(decoder: Decoder): ByteArray {
        val base64Decoded = decoder.decodeString()
        return base64.decode(base64Decoded)
    }
}

@Serializable
data class Value(
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val base64Input: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Value
        return base64Input.contentEquals(other.base64Input)
    }

    override fun hashCode(): Int {
        return base64Input.contentHashCode()
    }
}

fun main() {
    val string = "foo string"
    val value = Value(string.toByteArray())
    
    // Encodes the data class to a Base64 string
    val encoded = Json.encodeToString(value)
    println(encoded)
    // {"base64Input":"Zm9vIHN0cmluZw=="}

    // Decodes the Base64 string back to its original form
    val decoded = Json.decodeFromString<Value>(encoded)
    println(decoded.base64Input.decodeToString())
    // foo string
}
