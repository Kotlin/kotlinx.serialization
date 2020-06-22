/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlin.test.*

class BasicTypesSerializationTest : JsonTestBase() {

    val goldenValue = """
        {"unit":{},"boolean":true,"byte":10,"short":20,"int":30,"long":40,"float":50.1,"double":60.1,"char":"A","string":"Str0","enum":"POSITIVE","intData":{"intV":70},"unitN":null,"booleanN":null,"byteN":11,"shortN":21,"intN":31,"longN":41,"floatN":51.1,"doubleN":61.1,"charN":"B","stringN":"Str1","enumN":"NEUTRAL","intDataN":null,"listInt":[1,2,3],"listIntN":[4,5,null],"listNInt":[6,7,8],"listNIntN":[null,9,10],"listListEnumN":[["NEGATIVE",null]],"listIntData":[{"intV":1},{"intV":2},{"intV":3}],"listIntDataN":[{"intV":1},null,{"intV":3}],"tree":{"name":"root","left":{"name":"left","left":null,"right":null},"right":{"name":"right","left":{"name":"right.left","left":null,"right":null},"right":{"name":"right.right","left":null,"right":null}}},"mapStringInt":{"one":1,"two":2,"three":3},"mapIntStringN":{"0":null,"1":"first","2":"second"},"arrays":{"arrByte":[1,2,3],"arrInt":[100,200,300],"arrIntN":[null,-1,-2],"arrIntData":[{"intV":1},{"intV":2}]}}
    """.trimIndent()

    @Test
    fun testSerialization() = parametrizedTest { useStreaming ->
        val json = default.encodeToString(TypesUmbrella.serializer(), umbrellaInstance)
        assertEquals(goldenValue, json)
        val instance = default.decodeFromString(TypesUmbrella.serializer(), json, useStreaming)
        assertEquals(umbrellaInstance, instance)
        assertNotSame(umbrellaInstance, instance)
    }

    @Test
    fun testTopLevelPrimitive() = parametrizedTest { useStreaming ->
        testPrimitive(Unit, "{}", useStreaming)
        testPrimitive(false, "false", useStreaming)
        testPrimitive(1.toByte(), "1", useStreaming)
        testPrimitive(2.toShort(), "2", useStreaming)
        testPrimitive(3, "3", useStreaming)
        testPrimitive(4L, "4", useStreaming)
        testPrimitive(5.1f, "5.1", useStreaming)
        testPrimitive(6.1, "6.1", useStreaming)
        testPrimitive('c', "\"c\"", useStreaming)
        testPrimitive("string", "\"string\"", useStreaming)
    }

    private inline fun <reified T : Any> testPrimitive(primitive: T, expectedJson: String, useStreaming: Boolean) {
        val json = default.encodeToString(primitive, false)
        assertEquals(expectedJson, json)
        val instance = default.decodeFromString<T>(json, useStreaming)
        assertEquals(primitive, instance)
    }
}
