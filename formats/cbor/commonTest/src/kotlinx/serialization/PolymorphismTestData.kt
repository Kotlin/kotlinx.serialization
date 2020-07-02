/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.modules.*

@Serializable
abstract class SimpleAbstract

@Serializable
data class SimpleIntInheritor(val i: Int, val s: String) : SimpleAbstract()

@Serializable
data class SimpleStringInheritor(val s: String, val i: Int) : SimpleAbstract()

@Serializable
data class PolyBox(@Polymorphic val boxed: SimpleAbstract)

val SimplePolymorphicModule = SerializersModule {
    polymorphic(SimpleAbstract::class) {
        subclass(SimpleIntInheritor.serializer())
        subclass(SimpleStringInheritor.serializer())
    }
}

@Serializable
sealed class SimpleSealed {
    @Serializable
    public data class SubSealedA(val s: String) : SimpleSealed()

    @Serializable
    public data class SubSealedB(val i: Int) : SimpleSealed()
}

@Serializable
data class SealedBox(val boxed: List<SimpleSealed>)
