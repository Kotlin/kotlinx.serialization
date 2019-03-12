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

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlin.test.*

@Serializable
data class FooHolder(
    val someMetadata: Int,
    val payload: List<Foo>
)

@Serializable
sealed class Foo {
    @Serializable
    data class Bar(val bar: Int) : Foo()
    @Serializable
    data class Baz(val baz: Int) : Foo()
}


class SealedPolymorphismTest {
    @Test
    fun saveSealedClassesList() {
        val holder = FooHolder(42, listOf(Foo.Bar(1), Foo.Baz(2)))
        val s = Json.stringify(holder)
        assertEquals("""{"someMetadata":42,"payload":[["kotlinx.serialization.features.Foo.Bar",{"bar":1}],["kotlinx.serialization.features.Foo.Baz",{"baz":2}]]}""", s)
    }
}
