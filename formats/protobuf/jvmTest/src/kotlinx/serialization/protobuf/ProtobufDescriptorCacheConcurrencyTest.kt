/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlinx.serialization.modules.*
import java.util.concurrent.*
import kotlin.test.*

class ProtobufDescriptorCacheConcurrencyTest {

    @Serializable
    data class Leaf(@ProtoNumber(7) val name: String, @ProtoType(ProtoIntegerType.FIXED) val value: Int)

    @Serializable
    sealed interface Choice

    @Serializable
    data class LeafChoice(@ProtoNumber(20) val leaf: Leaf) : Choice

    @Serializable
    data class TextChoice(@ProtoNumber(21) val text: String) : Choice

    @Serializable
    data class Node(
        val id: Long,
        @ProtoPacked val packed: List<Int>,
        val leaves: List<Leaf>,
        val byName: Map<String, Leaf>,
        @ProtoOneOf val choice: Choice,
        @ProtoNumber(30) val child: Node? = null,
    )

    @Serializable
    data class WithUnknown(val id: Long, val unknown: ProtoUnknownFieldHolder)

    private fun node(i: Int): Node = Node(
        id = i.toLong(),
        packed = List(i % 5) { it * i },
        leaves = List(i % 3) { Leaf("l$it", it - i) },
        byName = mapOf("k$i" to Leaf("v", i)),
        choice = if (i % 2 == 0) LeafChoice(Leaf("c", i)) else TextChoice("t$i"),
        child = if (i % 4 == 0) node(i + 1) else null,
    )

    @Test
    fun testConcurrentEncodeAndDecodeOnSharedInstance() {
        val reference = ProtoBuf { serializersModule = EmptySerializersModule() }
        val values = List(64) { node(it) }
        val expected = values.map { reference.encodeToByteArray(it) }

        repeat(5) {
            val shared = ProtoBuf { }
            val threads = 8
            val start = CountDownLatch(1)
            val pool = Executors.newFixedThreadPool(threads)
            try {
                val futures = List(threads) { t ->
                    pool.submit<Unit> {
                        start.await()
                        repeat(200) { iteration ->
                            val index = (t * 31 + iteration) % values.size
                            val bytes = shared.encodeToByteArray(values[index])
                            assertContentEquals(expected[index], bytes)
                            assertEquals(values[index], shared.decodeFromByteArray<Node>(bytes))
                            val withUnknown = shared.decodeFromByteArray<WithUnknown>(bytes)
                            assertEquals(values[index], shared.decodeFromByteArray<Node>(shared.encodeToByteArray(withUnknown)))
                        }
                    }
                }
                start.countDown()
                futures.forEach { it.get(30, TimeUnit.SECONDS) }
            } finally {
                pool.shutdownNow()
            }
        }
    }
}
