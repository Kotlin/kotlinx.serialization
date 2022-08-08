/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.test.noLegacyJs
import kotlin.test.Test


class JsonHugeDataSerializationTest : JsonTestBase() {

    @Serializable
    private data class Node(
        val children: List<Node>?
    )

    private fun createNodes(count: Int, depth: Int): List<Node> {
        val ret = mutableListOf<Node>()
        if (depth == 0) return ret
        for (i in 0 until count) {
            ret.add(Node(createNodes(1, depth - 1)))
        }
        return ret
    }

    @Test
    fun test() = noLegacyJs {
        // create some huge instance
        val rootNode = Node(createNodes(1000, 10))

        //  Encoding will always be true for a standard `encodeToString` - we leave this assumption so as not to insert a huge string into the sources
        val expectedJson = Json.encodeToString(rootNode)

        assertJsonFormAndRestored(Node.serializer(), rootNode, expectedJson)
    }
}
