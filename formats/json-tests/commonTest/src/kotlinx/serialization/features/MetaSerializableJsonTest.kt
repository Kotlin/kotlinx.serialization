package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.test.*

@MetaSerializable
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
annotation class JsonComment(val comment: String)

@JsonComment("class_comment")
data class IntDataCommented(val i: Int)

class MetaSerializableJsonTest : JsonTestBase() {

    @Serializable
    data class Carrier(
        val plain: String,
        @JsonComment("string_comment") val commented: StringData,
        val intData: IntDataCommented
    )

    class CarrierSerializer : JsonTransformingSerializer<Carrier>(serializer()) {

        private val desc = Carrier.serializer().descriptor
        private fun List<Annotation>.comment(): String? = filterIsInstance<JsonComment>().firstOrNull()?.comment

        private val commentMap = (0 until desc.elementsCount).associateBy({ desc.getElementName(it) },
            { desc.getElementAnnotations(it).comment() ?: desc.getElementDescriptor(it).annotations.comment() })

        // NB: we may want to add this to public API
        private fun JsonElement.editObject(action: (MutableMap<String, JsonElement>) -> Unit): JsonElement {
            val mutable = this.jsonObject.toMutableMap()
            action(mutable)
            return JsonObject(mutable)
        }

        override fun transformDeserialize(element: JsonElement): JsonElement {
            return element.editObject { result ->
                for ((key, value) in result) {
                    commentMap[key]?.let {
                        result[key] = value.editObject {
                            it.remove("comment")
                        }
                    }
                }
            }
        }

        override fun transformSerialize(element: JsonElement): JsonElement {
            return element.editObject { result ->
                for ((key, value) in result) {
                    commentMap[key]?.let { comment ->
                        result[key] = value.editObject {
                            it["comment"] = JsonPrimitive(comment)
                        }
                    }
                }
            }
        }
    }

    @Test
    fun testMyJsonComment()  {
        assertJsonFormAndRestored(
            CarrierSerializer(),
            Carrier("plain", StringData("string1"), IntDataCommented(42)),
            """{"plain":"plain","commented":{"data":"string1","comment":"string_comment"},"intData":{"i":42,"comment":"class_comment"}}"""
        )
    }

}
