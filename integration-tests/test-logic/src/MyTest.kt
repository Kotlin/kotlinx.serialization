/*
 *  Copyright 2018 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 *  Copyright 2018 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import kotlinx.serialization.json.JSON
import kotlin.test.Test
import kotlin.test.assertEquals

/*
 *  Copyright 2018 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

class MyTest {
    @Test
    fun testWrite() {
        val d = MyData(10)
        val j = JSON.unquoted.stringify(MyData.serializer(), d)
        assertEquals("{x:10,y:foo,intList:[1,2,3]}", j)
    }

    @Test
    fun testRead() {
        val d = MyData(10, intList = listOf(1, 2))
        val json = JSON(unquoted = true, nonstrict = true)
        val j = json.parse(MyData.serializer(), "{x:10,y:foo,intList:[1,2],choice:LEFT}")
        assertEquals(d, j)
        val j2 = json.parse(MyData.serializer(), "{x:null,intList:[1,2,3],choice:LEFT}")
        assertEquals(MyData(null), j2)
    }
}
