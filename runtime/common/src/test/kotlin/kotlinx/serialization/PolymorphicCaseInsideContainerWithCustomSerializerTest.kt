/*
 * Copyright 2017 JetBrains s.r.o.
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

import kotlinx.serialization.internal.SerialClassDescImpl
import kotlinx.serialization.json.JSON
import kotlin.test.Test
import kotlin.test.assertEquals


data class Foo(val id: String)

sealed class PolymorphicContainer {
    abstract val id: String

    data class Completed(val item: Foo): PolymorphicContainer() {
        override val id: String = item.id
    }

    data class Incomplete(override val id: String): PolymorphicContainer()
}

@Serializable
data class Response(
    val items: List<PolymorphicContainer>
)

object ContainerSerializer : KSerializer<PolymorphicContainer> {
    override val serialClassDesc: KSerialClassDesc = SerialClassDescImpl("kotlinx.serialization.PolymorphicContainer")

    override fun load(input: KInput): PolymorphicContainer {
        val id = input.readStringValue()
        return PolymorphicContainer.Incomplete(id)
    }

    override fun save(output: KOutput, obj: PolymorphicContainer) {
        output.writeStringValue(obj.id)
    }
}

class PolymorphicCaseInsideContainerWithCustomSerializerTest {
    private val context = SerialContext().apply {
        registerSerializer(PolymorphicContainer::class, ContainerSerializer)
        registerSerializer(PolymorphicContainer.Completed::class, ContainerSerializer as KSerializer<PolymorphicContainer.Completed>)
        registerSerializer(PolymorphicContainer.Incomplete::class, ContainerSerializer as KSerializer<PolymorphicContainer.Incomplete>)
    }

    private val contextualJSON = JSON(context = context)

    @Test
    fun serialize() {
        val testResponse = Response(
            items = listOf(
                PolymorphicContainer.Completed(Foo("foo")),
                PolymorphicContainer.Incomplete("bar")
            )
        )
        val json = contextualJSON.stringify(testResponse)

        assertEquals("{\"items\":[\"foo\",\"bar\"]}", json)
    }
}
