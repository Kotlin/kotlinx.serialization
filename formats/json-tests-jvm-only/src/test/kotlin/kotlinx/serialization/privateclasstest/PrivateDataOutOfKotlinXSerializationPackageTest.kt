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

package kotlinx.serialization.privateclasstest

import kotlinx.serialization.*
import kotlinx.serialization.SerializeFlatTest.Inp
import kotlinx.serialization.SerializeFlatTest.Out
import org.junit.Test

// Serializable data class with private visibility that lays out of serialization library package

@Serializable
private data class DataPrivate(
        val value1: String,
        val value2: Int
) {
    companion object
}

class PrivateClassOutOfSerializationLibraryPackageTest {

    @Test
    fun testDataPrivate() {
        val out = Out("privateclasstest.DataPrivate")
        out.encodeSerializableValue(serializer(), DataPrivate("s1", 42))
        out.done()

        val inp = Inp("privateclasstest.DataPrivate")
        val data = inp.decodeSerializableValue(serializer<DataPrivate>())
        inp.done()
        assert(data.value1 == "s1" && data.value2 == 42)
    }
}
