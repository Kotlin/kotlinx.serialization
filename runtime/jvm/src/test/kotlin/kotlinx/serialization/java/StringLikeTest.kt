/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:ContextualSerialization(URI::class, URL::class, UUID::class, StringBuilder::class)

package kotlinx.serialization.java

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.junit.*
import org.junit.Test
import java.net.*
import java.util.*
import kotlin.test.*

class StringLikeTest : JsonTestBase() {

    @Serializable
    private class StringLike(val uri: URI, val url: URL, val uuid: UUID, val sb: StringBuilder) {
        // StringBuilder doesn't have equals/hc
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as StringLike

            if (uri != other.uri) return false
            if (url != other.url) return false
            if (uuid != other.uuid) return false
            if (sb.toString() != other.sb.toString()) return false

            return true
        }

        override fun hashCode(): Int {
            var result = uri.hashCode()
            result = 31 * result + url.hashCode()
            result = 31 * result + uuid.hashCode()
            result = 31 * result + sb.toString().hashCode()
            return result
        }
    }

    @Before
    fun setUp() {
        unquoted.install(JavaTypesModule)
    }

    @Test
    fun testSerializer() = parametrizedTest { useStreaming ->
        val stringLike = StringLike(URI("file://empty"), URL("https://bit.ly/IqT6zt"),
            UUID.fromString("784a9cf4-28ec-4ceb-b478-6d02542558ab"), StringBuilder().append("..."))
        val serialized = unquoted.stringify(stringLike, useStreaming)
        assertEquals("{uri:\"file://empty\",url:\"https://bit.ly/IqT6zt\"," +
                "uuid:784a9cf4-28ec-4ceb-b478-6d02542558ab,sb:...}", serialized)
        assertEquals(stringLike, unquoted.parse(serialized, useStreaming))
    }
}
