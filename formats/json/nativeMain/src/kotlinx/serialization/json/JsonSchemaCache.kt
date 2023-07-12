/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.json.internal.*
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.*
import kotlin.random.*
import kotlin.native.concurrent.*

/**
 * This maps emulate thread-locality of DescriptorSchemaCache for Native.
 *
 * Custom JSON instances are considered thread-safe (in JVM) and can be frozen and transferred to different workers (in Native).
 * Therefore, DescriptorSchemaCache should be either a concurrent freeze-aware map or thread local.
 * Each JSON instance have it's own schemaCache, and it's impossible to use @ThreadLocal on non-global vals.
 * Thus we make @ThreadLocal this special map: it provides schemaCache for a particular Json instance
 * and should be used instead of a member `_schemaCache` on Native.
 *
 * To avoid memory leaks (when Json instance is no longer in use), WeakReference is used with periodical self-cleaning.
 */
@ThreadLocal
private val jsonToCache: MutableMap<WeakJson, DescriptorSchemaCache> = mutableMapOf()

/**
 * Because WeakReference itself does not have proper equals/hashCode
 */
@OptIn(ExperimentalNativeApi::class)
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
 * Accessor for DescriptorSchemaCache
 */
internal actual val Json.schemaCache: DescriptorSchemaCache
    get() = jsonToCache.getOrPut(WeakJson(this)) { DescriptorSchemaCache() }.also { cleanUpWeakMap() }
