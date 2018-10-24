package kotlinx.serialization

import kotlinx.serialization.json.JSON
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.test.Test
import kotlin.test.assertEquals

enum class SomeEnum { ALPHA, BETA, GAMMA }

@Serializable
data class WithUnions(@SerialId(5) val s: String,
                      @SerialId(6) val e: SomeEnum = SomeEnum.ALPHA,
                      @SerialId(7) val i: Int = 42)

class UnionEnumTest {
    @Test
    fun simpleEnum() {
        val data = WithUnions("foo", SomeEnum.BETA)
        val json = JSON.stringify(data)
        val restored = JSON.parse<WithUnions>(json)
        assertEquals(data, restored)
    }

    @Test
    fun enumInProto() {
        val data = WithUnions("foo", SomeEnum.BETA)
        val hex = ProtoBuf.dumps(data)
        val restored = ProtoBuf.loads<WithUnions>(hex)
        assertEquals(data, restored)
    }
}
