package kotlinx.serialization

import kotlin.reflect.KClass

annotation class SerializableSchema

interface KSchemaSerializer {
    fun saveSchema(to: SchemaOutput)
}

data class FieldDescriptor(val optional: Boolean, val nullable: Boolean) {
    companion object {
        val DEFAULT = FieldDescriptor(false, false)
    }
} /** annotations, serial tags, etc **/


interface SchemaOutput {
    fun writePrimitive(desc: KSerialClassDesc, index: Int, fieldClass: KClass<*>, fieldDescriptor: FieldDescriptor = FieldDescriptor.DEFAULT)

    fun writeNested(desc: KSerialClassDesc, index: Int, nested: KSchemaSerializer, fieldDescriptor: FieldDescriptor = FieldDescriptor.DEFAULT) {
        nested.saveSchema(this)
    }

    fun writeBegin(desc: KSerialClassDesc, vararg typeParams: KSchemaSerializer): SchemaOutput
    fun writeEnd(desc: KSerialClassDesc)
}



class DefinitionWriter private constructor(val sb: StringBuilder = StringBuilder()): SchemaOutput {
    private operator fun StringBuilder.plusAssign(what: String) {
        this.append(" ".repeat(4 * identLevel))
        this.append(what)
        this.append("\n")
    }

    private var identLevel = 0

    fun result() = sb.toString()

    override fun writePrimitive(
        desc: KSerialClassDesc,
        index: Int,
        fieldClass: KClass<*>,
        fieldDescriptor: FieldDescriptor
    ) {
        val name = desc.getElementName(index)
        val className = fieldClass.simpleName
        sb += "$name: $className"
    }

    override fun writeBegin(desc: KSerialClassDesc, vararg typeParams: KSchemaSerializer): SchemaOutput {
        sb += "${desc.name} {"
        identLevel++
        return this
    }

    override fun writeEnd(desc: KSerialClassDesc) {
        identLevel--
        sb += "}"
    }

    companion object {
        fun saveSchema(saver: KSchemaSerializer): String {
            val writer = DefinitionWriter()
            saver.saveSchema(writer)
            return writer.result()
        }
    }
}



class LegacySchemaSaver: NamedValueOutput() {
    override fun composeName(parentName: String, childName: String): String {
        return childName
    }

    override fun writeBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KOutput {
        println("${desc.name} {")
        return super.writeBegin(desc, *typeParams)
    }

    override fun writeTaggedInt(tag: String, value: Int) {
        println("$tag: Int")
    }

    override fun writeFinished(desc: KSerialClassDesc) {
        println("}")
    }
}


