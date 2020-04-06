/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

@Serializable
data class IntData(val intV: Int)

@Serializable
data class StringData(val data: String)

enum class SampleEnum { OptionA, OptionB, OptionC }

@Serializable
data class Box<T>(val boxed: T)

@Serializable
sealed class SimpleSealed {
    @Serializable
    public data class SubSealedA(val s: String) : SimpleSealed()

    @Serializable
    public data class SubSealedB(val i: Int) : SimpleSealed()
}

@Serializable
object SampleObject {
    val state: String = "myState"
}
