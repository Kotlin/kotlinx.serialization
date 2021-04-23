package kotlinx.serialization.protobuf.schema

import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.serializer
import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.reflect.KClass

private const val RESOURCES_ROOT_PATH = "formats/protobuf/jvmTest/resources"

// Regenerate all proto file for tests.
private fun main() {
    regenerateAllProtoFiles()
}

private fun regenerateAllProtoFiles() {
    generateProtoFile(
        GenerationTest.OptionsClass::class,
        mapOf("java_package" to "api.proto", "java_outer_classname" to "Outer")
    )

    commonClasses.forEach { generateProtoFile(it) }
    generateCommonProtoFile(commonClasses)
}

private fun generateProtoFile(clazz: KClass<*>, options: Map<String, String> = emptyMap()
) {
    generateSchemaFile("${clazz.simpleName}.proto", listOf(clazz.serializer().descriptor), options)
}

private fun generateCommonProtoFile(classes: List<KClass<*>>) {
    val descriptors = classes.map { it.serializer().descriptor }.toList()
    generateSchemaFile(COMMON_SCHEMA_FILE_NAME, descriptors)
}

private fun generateSchemaFile(
    fileName: String,
    descriptors: List<SerialDescriptor>,
    options: Map<String, String> = emptyMap()
) {
    val filePath = "$RESOURCES_ROOT_PATH/$fileName"
    val file = File(filePath)
    val schema = ProtoBufSchemaGenerator.generateSchemaText(descriptors, TARGET_PACKAGE, options)
    file.writeText(schema, StandardCharsets.UTF_8)
}
