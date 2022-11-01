package kotlinx.serialization.hocon

import kotlinx.serialization.*
import kotlinx.serialization.hocon.serializers.JBeanSerializer.Companion.jBeanSerializer
import kotlinx.serialization.modules.*
import org.junit.Assert.*
import org.junit.Test

class HoconJBeanTest {

    @Serializable
    data class TestData(@Contextual val d: TestJavaBean)
    @Serializable
    data class TestNullableData(@Contextual val d: TestJavaBean?)
    @Serializable
    data class ConfigList(val ld: List<@Contextual TestJavaBean>)
    @Serializable
    data class ConfigMap(val mp: Map<String, @Contextual TestJavaBean>)

    @Serializable
    data class Complex(
        val i: Int,
        val s: TestData,
        val n: TestNullableData,
        val l: List<TestData>,
        val ln: List<TestNullableData>,
        val f: Boolean,
        val ld: List<@Contextual TestJavaBean>,
        val mp: Map<String, @Contextual TestJavaBean>
    )

    private val strConfig = """
        d: {
            name = Alex
            age = 27
        }
        """.trimIndent()
    private val bean = TestJavaBean().apply {
        name = "Alex"
        age = 27
    }

    private inline fun <reified T> deserializeJBeanConfig(
        configString: String,
        deserializer: DeserializationStrategy<T>,
    ): T = deserializeConfig(configString, deserializer, false, serializersModuleOf(jBeanSerializer<TestJavaBean>()))

    @Test
    fun testDeserializeJBean() {
        val obj = deserializeJBeanConfig(strConfig, TestData.serializer())
        assertEquals(TestData(bean), obj)
    }

    @Test
    fun testDeserializeNullableJBean() {
        var obj = deserializeJBeanConfig("d: null", TestNullableData.serializer())
        assertNull(obj.d)
        obj = deserializeJBeanConfig(strConfig, TestNullableData.serializer())
        assertEquals(TestNullableData(bean), obj)
    }

    @Test
    fun testDeserializeListOfJBean() {
        val obj = deserializeJBeanConfig("""
            ld: [
                { name = Alex, age = 27 },
                { name = Alex, age = 27 }
            ]
        """.trimIndent(), ConfigList.serializer())
        assertEquals(listOf(bean, bean), obj.ld)
    }

    @Test
    fun testDeserializeMapOfJBean() {
        val obj = deserializeJBeanConfig("""
            mp: { first = { name = Alex, age = 27 }, second = { name = Alex, age = 27 } }
        """.trimIndent(), ConfigMap.serializer())
        assertEquals(mapOf("first" to bean, "second" to bean), obj.mp)
    }

    @Test
    fun testDeserializeComplexJBean() {
        val obj = deserializeJBeanConfig("""
            i = 6
            s: { d: { name = Alex, age = 27 } }
            n: { d: null }
            l: [ { d: { name = Alex, age = 27 } }, { d: { name = Alex, age = 27 } } ]
            ln: [ { d: null }, { d: { name = Alex, age = 27 } } ]
            f = true
            ld: [ { name = Alex, age = 27 }, { name = Alex, age = 27 } ]
            mp: { first = { name = Alex, age = 27 } }
        """.trimIndent(), Complex.serializer())
        assertEquals(bean, obj.s.d)
        assertNull(obj.n.d)
        assertEquals(listOf(TestData(bean), TestData(bean)), obj.l)
        assertEquals(listOf(TestNullableData(null), TestNullableData(bean)), obj.ln)
        assertTrue(obj.f)
        assertEquals(listOf(bean, bean), obj.ld)
        assertEquals(mapOf("first" to bean), obj.mp)
    }
}
