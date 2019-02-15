/*
 * Copyright 2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.serialization

import kotlin.test.Test
import kotlin.test.assertEquals

@SerialInfo
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
annotation class CustomAnnotation(val value: String)

private fun List<Annotation>.getCustom() = filterIsInstance<CustomAnnotation>().single().value

class SerialNamesTest {

    @Serializable
    @SerialName("MyClass")
    @CustomAnnotation("onClass")
    data class WithNames(val a: Int, @CustomAnnotation("onProperty") @SerialName("b") val veryLongName: String)

    @Test
    fun haveCustomPropertyName() {
        val desc = WithNames.serializer().descriptor
        val b = desc.getElementName(1)
        assertEquals("b", b)
    }

    @Test
    fun haveCustomClassName() {
        val desc = WithNames.serializer().descriptor
        val name = desc.name
        assertEquals("MyClass", name)
    }

    @Test
    fun haveCustomAnnotationOnProperty() {
        val desc: SerialDescriptor = WithNames.serializer().descriptor
        val b = desc.getElementAnnotations(1).getCustom()
        assertEquals("onProperty", b)
    }

    @Test
    fun haveCustomAnnotationOnClass() {
        val desc: SerialDescriptor = WithNames.serializer().descriptor
        val name = desc.getEntityAnnotations().getCustom()
        assertEquals("onClass", name)
    }
}
