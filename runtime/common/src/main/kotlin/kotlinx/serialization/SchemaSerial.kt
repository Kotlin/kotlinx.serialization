package kotlinx.serialization

interface ExtendedSerializer<T>: KSerializer<T> {
    fun saveSchema(output: KOutput)
}

class ExtendedSchemaOutput: ElementValueOutput() {
    override fun <T> writeSerializableValue(saver: KSerialSaver<T>, value: T) {
        if (saver !is ExtendedSerializer) throw IllegalArgumentException("Saving schema is not supported by $saver")
        saver.saveSchema(this)
    }

    override fun writeElement(desc: KSerialClassDesc, index: Int): Boolean {
        println(desc.getElementName(index))
        return true
    }

    override fun writeNonSerializableValue(value: Any) {
    }

    override fun writeBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KOutput {
       println("${desc.name} {")
        return this
    }

    override fun writeEnd(desc: KSerialClassDesc) {
        println("}")
    }
}
