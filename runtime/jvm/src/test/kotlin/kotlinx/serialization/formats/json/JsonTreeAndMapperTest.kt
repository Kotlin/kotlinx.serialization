package kotlinx.serialization.formats.json

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals

@Serializable
data class Payload(val from: Long, val to: Long, val msg: String)

sealed class DummyEither {
    data class Left(val errorMsg: String): DummyEither()
    data class Right(val data: Payload): DummyEither()
}

@Serializer(forClass = DummyEither::class)
object EitherSerializer: KSerializer<DummyEither> {
    override fun load(input: KInput): DummyEither {
        val jsonReader = input as? JSON.JsonInput
                ?: throw SerializationException("This class can be loaded only by JSON")
        val tree = jsonReader.readAsTree() as? JsonObject
                ?: throw SerializationException("Expected JSON object")
        if ("error" in tree) return DummyEither.Left(tree.getAsValue("error")?.str!!)
        return DummyEither.Right(JsonTreeMapper().readTree(tree, Payload.serializer()))
    }

    override fun save(output: KOutput, obj: DummyEither) {
        val tree = when (obj) {
            is DummyEither.Left -> JsonObject(mapOf("error" to JsonString(obj.errorMsg)))
            is DummyEither.Right -> JsonTreeMapper().writeTree(obj.data, Payload.serializer())
        }
        val jsonWriter = output as? JSON.JsonOutput
                ?: throw SerializationException("This class can be saved only by JSON")
        jsonWriter.writeTree(tree)
    }
}

@Serializable
data class Event(
    val id: Int,
    @Serializable(with=EitherSerializer::class) val payload: DummyEither,
    val timestamp: Long
)

class JsonTreeAndMapperTest {
    val inputData = """{"id":0,"payload":{"msg": "Hello world", "from": 42, "to": 43},"timestamp":1000}"""
    val inputError = """{"id":1,"payload":{"error": "Connection timed out"},"timestamp":1001}"""

    @Test
    fun testParseData() {
        val ev = JSON.parse(Event.serializer(), inputData)
        with(ev) {
            assertEquals(0, id)
            assertEquals(DummyEither.Right(Payload(42, 43, "Hello world")), payload)
            assertEquals(1000, timestamp)
        }
    }

    @Test
    fun testParseError() {
        val ev = JSON.parse(Event.serializer(), inputError)
        with(ev) {
            assertEquals(1, id)
            assertEquals(DummyEither.Left("Connection timed out"), payload)
            assertEquals(1001, timestamp)
        }
    }

    @Test
    fun testWriteData() {
        val outputData = Event(0, DummyEither.Right(Payload(42, 43, "Hello world")), 1000)
        val ev = JSON.stringify(Event.serializer(), outputData)
        assertEquals(inputData, ev)
    }

    @Test
    fun testWriteError() {
        val outputError = Event(1, DummyEither.Left("Connection timed out"), 1001)
        val ev = JSON.stringify(Event.serializer(), outputError)
        assertEquals(inputError, ev)
    }

}
