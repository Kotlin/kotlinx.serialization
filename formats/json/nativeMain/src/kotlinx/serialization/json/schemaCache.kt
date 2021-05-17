/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.json.internal.*
import kotlin.native.ref.*
import kotlin.random.*

@ThreadLocal
private val jsonToCache: MutableMap<WeakJson, DescriptorSchemaCache> = mutableMapOf()

/**
 * Because WeakReference itself does not have equals/hashCode
 */
private class WeakJson(json: Json) {
    private val ref = WeakReference(json)
    private val initialHashCode = json.hashCode()

    val isDead: Boolean get() = ref.get() == null

    override fun equals(other: Any?): Boolean {
        if (other !is WeakJson) return false
        val thiz = this.ref.get() ?: return false
        val that = other.ref.get() ?: return false
        return thiz == that
    }

    override fun hashCode(): Int = initialHashCode

}

/**
 * To maintain O(1) access, we cleanup the table from dead references with 1/size probability
 */
private fun cleanUpWeakMap() {
    val size = jsonToCache.size
    if (size <= 10) return // 10 is arbitrary small number to ignore polluting
    // Roll 1/size probability
    if (Random.nextInt(0, size) == 0) {
        val iter = jsonToCache.iterator()
        while (iter.hasNext()) {
            if (iter.next().key.isDead) iter.remove()
        }
    }
}

/**
 * Emulate thread locality of cache on Native
 */
internal actual val Json.schemaCache: DescriptorSchemaCache
    get() = jsonToCache.getOrPut(WeakJson(this)) { DescriptorSchemaCache() }.also { cleanUpWeakMap() }
