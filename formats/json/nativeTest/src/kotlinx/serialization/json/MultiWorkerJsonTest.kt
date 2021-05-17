/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlin.native.concurrent.*
import kotlin.test.*

class MultiWorkerJsonTest {
    @Serializable
    data class PlainOne(val one: Int)

    @Serializable
    data class PlainTwo(val two: Int)

    private fun doTest(json: () -> Json) {
        val worker = Worker.start()
        val operation = {
            for (i in 0..999) {
                assertEquals(PlainOne(42), json().decodeFromString("""{"one":42,"two":239}"""))
            }
        }
        worker.executeAfter(1000, operation.freeze())
        for (i in 0..999) {
            assertEquals(PlainTwo(239), json().decodeFromString("""{"one":42,"two":239}"""))
        }
        worker.requestTermination()
    }


    @Test
    fun testJsonIsFreezeSafe() {
        val json = Json {
            isLenient = true
            ignoreUnknownKeys = true
            useAlternativeNames = true
        }
        // reuse instance
        doTest { json }
    }

    @Test
    fun testJsonInstantiation() {
        // create new instanceEveryTime
        doTest {
            Json {
                isLenient = true
                ignoreUnknownKeys = true
                useAlternativeNames = true
            }
        }
    }
}
