/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.test

import java.lang.reflect.*

// Same classes are present in SerializerByTypeTest.kt,
// but it seems that json-jvm-test does not depend on core-jvm-test

@PublishedApi
internal open class TypeBase<T>

public inline fun <reified T> typeTokenOf(): Type {
    val base = object : TypeBase<T>() {}
    val superType = base::class.java.genericSuperclass!!
    return (superType as ParameterizedType).actualTypeArguments.first()!!
}
