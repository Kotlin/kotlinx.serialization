/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.internal.UnitSerializer
import kotlin.test.Test

class SealedGenericClassesTest {
    interface Output

    @Serializable
    data class Something(val s: String) : Output

    @Serializable
    sealed class Query<T : Output> {
        @Serializable
        data class SimpleQuery<T: Output>(val rawQuery: String) : Query<T>()
    }

    @Serializable
    sealed class Fetcher<T : Output> {
        abstract val query: Query<T>

        @Serializable
        data class SomethingFetcher(override val query: Query<Something>) : Fetcher<Something>()
    }

    // Test that compilation and retrieval is successful
    @Test
    fun testQuery() {
        Query.SimpleQuery.serializer(String.serializer())
        Query.serializer(UnitSerializer)
    }

    @Test
    fun testFetcher() {
        Fetcher.SomethingFetcher.serializer()
        Fetcher.serializer(Something.serializer())
    }
}
