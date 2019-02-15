/*
 * Copyright 2019 JetBrains s.r.o.
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

@file:Suppress("EqualsOrHashCode")

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlin.test.*

@Serializable
abstract class AbstractSerializable {
    public abstract val rootState: String // no backing field

    val publicState: String = "A"
}

@Serializable
open class SerializableBase: AbstractSerializable() {


    private val privateState: String = "B" // still should be serialized

    @Transient
    private val privateTransientState = "C" // not serialized: explicitly transient

    val notAState: String // not serialized: no backing field
        get() = "D"

    override val rootState: String
        get() = "E" // still not serializable

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SerializableBase) return false

        if (privateState != other.privateState) return false
        if (privateTransientState != other.privateTransientState) return false

        return true
    }
}

@Serializable
class Derived(val derivedState: Int): SerializableBase() {
    override val rootState: String = "foo" // serializable!

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Derived) return false
        if (!super.equals(other)) return false

        if (derivedState != other.derivedState) return false
        if (rootState != other.rootState) return false

        return true
    }
}

@Serializable
open class Base1(open var state1: String) {
    override fun toString(): String {
        return "Base1(state1='$state1')"
    }
}

@Serializable
class Derived2(@SerialName("state2") override var state1: String): Base1(state1) {
    override fun toString(): String {
        return "Derived2(state1='$state1')"
    }
}

class InheritanceTest {
    @Test
    fun canBeSerializedAsDerived() {
        val derived = Derived(42)
        val msg = Json.stringify(Derived.serializer(), derived)
        assertEquals("""{"publicState":"A","privateState":"B","derivedState":42,"rootState":"foo"}""", msg)
        val d2 = Json.parse(Derived.serializer(), msg)
        assertEquals(derived, d2)
    }

    @Test
    fun canBeSerializedAsParent() {
        val derived = Derived(42)
        val msg = Json.stringify(SerializableBase.serializer(), derived)
        assertEquals("""{"publicState":"A","privateState":"B"}""", msg)
        val d2 = Json.parse(SerializableBase.serializer(), msg)
        assertEquals(SerializableBase(), d2)
        // no derivedState
        assertFailsWith<MissingFieldException> { Json.parse(Derived.serializer(), msg) }
    }

    @Test
    fun testWithOpenProperty() {
        val d = Derived2("foo")
        val msgFull = Json.stringify(Derived2.serializer(), d)
        assertEquals("""{"state1":"foo","state2":"foo"}""", msgFull)
        assertEquals("""{"state1":"foo"}""", Json.stringify(Base1.serializer(), d))
        val restored = Json.parse(Derived2.serializer(), msgFull)
        val restored2 = Json.parse(Derived2.serializer(), """{"state1":"bar","state2":"foo"}""") // state1 is ignored anyway
        assertEquals("""Derived2(state1='foo')""", restored.toString())
        assertEquals("""Derived2(state1='foo')""", restored2.toString())
    }
}




