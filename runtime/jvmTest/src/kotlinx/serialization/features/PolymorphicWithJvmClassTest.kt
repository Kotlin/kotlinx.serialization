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
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals

class PolymorphicWithJvmClassTest {
    @Serializable
    data class DateWrapper(@SerialId(1) @Serializable(with = PolymorphicSerializer::class) val date: Date)

    @Test
    fun testPolymorphicWrappedOverride() {
        val protobuf = ProtoBuf(context = SerializersModule { polymorphic(Date::class, DateSerializer) })
        val obj = DateWrapper(Date())
        val bytes = protobuf.dumps(obj)
        val restored = protobuf.loads<DateWrapper>(bytes)
        assertEquals(obj, restored)
    }
}
