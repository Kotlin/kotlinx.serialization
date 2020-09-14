/*
 * Copyright 2017-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.polymorphic

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*

@Serializable
internal open class InnerBase

internal interface OuterBase

@Serializable
internal data class InnerImpl(val field: Int, val str: String = "default", val nullable: Int? = null) : InnerBase()

@Serializable
internal data class InnerImpl2(val field: Int) : InnerBase()

@Serializable
internal data class InnerBox(@Polymorphic val base: InnerBase)

@Serializable
internal data class InnerNullableBox(@Polymorphic val base: InnerBase?)

@Serializable
internal data class OuterImpl(@Polymorphic val base: InnerBase, @Polymorphic val base2: InnerBase) : OuterBase

@Serializable
internal data class OuterNullableImpl(@Polymorphic val base: InnerBase?, @Polymorphic val base2: InnerBase?) : OuterBase

@Serializable
internal data class OuterBox(@Polymorphic val outerBase: OuterBase, @Polymorphic val innerBase: InnerBase)

@Serializable
internal data class OuterNullableBox(@Polymorphic val outerBase: OuterBase?, @Polymorphic val innerBase: InnerBase?)


internal val polymorphicTestModule = SerializersModule {
    polymorphic(InnerBase::class) {
        subclass(InnerImpl.serializer())
        subclass(InnerImpl2.serializer())
    }

    polymorphic(OuterBase::class) {
        subclass(OuterImpl.serializer())
        subclass(OuterNullableImpl.serializer())
    }
}

internal val polymorphicJson = Json {
    serializersModule = polymorphicTestModule
}

internal val polymorphicRelaxedJson = Json {
    isLenient = true
    serializersModule = polymorphicTestModule
}
