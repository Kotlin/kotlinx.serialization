import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.asTypeName
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import kotlinx.serialization.sourcegen.*
import java.io.File

open class DataClassesGenerator: DefaultTask() {
    var outputDir: File = File(project.projectDir, "src/main/kotlin")

    @TaskAction
    fun run() {
        saveFile(outputDir, "com.mycompany.model", "Model") {
            serializableClass("MyData") {
                dataClass = true
                property("x", Int::class.asTypeName().asNullable())
                property("y", String::class) {
                    optional = true
                    defaultValue = "\"foo\""
                }
                property("intList", ParameterizedTypeName.get(List::class, Int::class)) {
                    defaultValue = "listOf(1,2,3)"
                }
                property("trans", Int::class) {
                    defaultValue = "42"
                    transient = true
                }
            }
        }
    }
}
