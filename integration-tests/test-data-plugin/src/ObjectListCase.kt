// Auto-generated file, do not modify!
import kotlin.Int
import kotlin.String
import kotlin.collections.List
import kotlinx.serialization.Optional
import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable

@Serializable
data class Data(@SerialId(1)
val a: Int, @SerialId(2)
val b: String)

@Serializable
data class DataList(@Optional
@SerialId(1)
val list: List<Data> = emptyList())
