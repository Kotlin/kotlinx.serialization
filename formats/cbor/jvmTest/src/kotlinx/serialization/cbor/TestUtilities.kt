/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.dataformat.cbor.*
import com.fasterxml.jackson.module.kotlin.*
import kotlinx.serialization.*

internal val cborJackson = ObjectMapper(CBORFactory()).apply { registerKotlinModule() }

internal inline fun <reified T : Any> dumpCborCompare(it: T, alwaysPrint: Boolean = false): Boolean {
    var parsed: T?
    val c = try {
        val bytes = Cbor.encodeToByteArray(it)
        parsed = cborJackson.readValue<T>(bytes)
        it == parsed
    } catch (e: Exception) {
        e.printStackTrace()
        parsed = null
        false
    }
    if (!c || alwaysPrint) println("Expected: $it\nfound: $parsed")
    return c
}

internal inline fun <reified T: Any> readCborCompare(it: T, alwaysPrint: Boolean = false): Boolean {
    var obj: T?
    val c = try {
        val hex = cborJackson.writeValueAsBytes(it)
        obj = Cbor.decodeFromByteArray(hex)
        obj == it
    } catch (e: Exception) {
        obj = null
        e.printStackTrace()
        false
    }
    if (!c || alwaysPrint) println("Expected: $it\nfound: $obj")
    return c
}
