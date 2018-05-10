package kotlinx.serialization

import kotlinx.serialization.internal.SchemaSerializer
import kotlinx.serialization.json.JSON
import kotlin.test.Test

class SchemaSaverTest {

    @Serializable
    @SerializableSchema
    data class Message(val id: Int, val payload: Data) {
        @Serializer(forClass = Message::class)
        companion object : KSchemaSerializer, ExtendedSerializer<Message> {
            override fun saveSchema(to: SchemaOutput) {
                val desc = serialClassDesc
                val output = to.writeBegin(desc)
                output.writePrimitive(desc, 0, Int::class)
                output.writeNested(desc, 1, Data.Companion)
                output.writeEnd(desc)
            }

            override fun saveSchema(output: KOutput) {
                val desc = serialClassDesc
                val output = output.writeBegin(desc)
                output.writeIntElementValue(desc, 0, 0)
                output.writeSerializableElementValue(desc, 1, SchemaSerializer(Data), null)
                output.writeEnd(desc)
            }
        }
    }

    @Serializable
    @SerializableSchema
    data class Data(val message: String, val timestamp: Long) {
        @Serializer(forClass = Data::class)
        companion object : KSchemaSerializer, ExtendedSerializer<Data> {
            override fun saveSchema(to: SchemaOutput) {
                val desc = serialClassDesc
                val output = to.writeBegin(desc)
                output.writePrimitive(desc, 0, String::class)
                output.writePrimitive(desc, 1, Long::class)
                output.writeEnd(desc)
            }

            override fun saveSchema(output: KOutput) {
                val desc = serialClassDesc
                val output = output.writeBegin(desc)
                output.writeStringElementValue(desc, 0, "")
                output.writeLongElementValue(desc, 1, 0)
                output.writeEnd(desc)
            }
        }
    }

    @Serializable
    data class MyInts(val a: Int, val kek: Int)

    @Test
    fun legacySchema() {
        val ints = MyInts(0, 0)
        val out = LegacySchemaSaver()
        out.write(ints)
    }

    @Test
    fun modernSchema() {
        val msg = Message(1, Data("Hello", 1024))
        val schema = DefinitionWriter.saveSchema(Message.Companion)
        println(JSON.stringify(msg))
        println(schema)
    }

    @Test
    fun mixedSchema() {
        val msg = Message(1, Data("Hello", 1024))
        val schema = ExtendedSchemaOutput()
        println(JSON.stringify(msg))
        schema.write(Message.Companion, msg)
    }
}
