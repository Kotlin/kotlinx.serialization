// Auto-generated file, do not modify!
import kotlin.Int
import kotlin.String
import kotlin.collections.List
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class MyData(
        val x: Int?,
        @Optional
        val y: String = "foo",
        val intList: List<Int> = listOf(1,2,3),
        @Transient
        val trans: Int = 42
)
