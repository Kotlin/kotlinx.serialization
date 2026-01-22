/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package not.kotlinx.serialization

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Test

class ComparisonTest {
    @Serializable
    class A(val a: Int) {
        init {
//            require(a.toIntOrNull() != null)
        }
    }

    @Serializable
    class Hold(val map: Map<String, A>)

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun a() {
        val input = """{"a":"not int"}"""
        println("// kotlinx.serialization:")
        println(runCatching { Json.decodeFromString<A>(input) }.exceptionOrNull())
        println("// Moshi:")
        println(runCatching {
            Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build().adapter<A>().fromJson(input)
        }.exceptionOrNull())
        println("// Jackson:")
        println(runCatching { jacksonObjectMapper().readValue(input, A::class.java) }.exceptionOrNull())
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun hold() {
        val input = """{"map":{"my ssn": {"a": "not int"}}}"""
        println("// kotlinx.serialization:")
        println(runCatching { Json.decodeFromString<Hold>(input) }.exceptionOrNull())
        println("// Moshi:")
        println(runCatching {
            Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build().adapter<Hold>().fromJson(input)
        }.exceptionOrNull())
        println("// Jackson:")
        println(runCatching { jacksonObjectMapper().readValue(input, Hold::class.java) }.exceptionOrNull())
    }
}
