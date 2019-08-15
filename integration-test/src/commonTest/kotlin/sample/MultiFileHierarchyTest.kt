@file:UseExperimental(UnstableDefault::class)
/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package sample

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlin.test.Test

@Serializable
class StubConcreteClass : AbstractBase()

class AbstractBaseTest {
    @Test
    fun concreteClass_test() {
        val concrete = ConcreteClass()
        val serialized: String = Json.stringify(ConcreteClass.serializer(), concrete)
        val parsed: ConcreteClass = Json.parse(ConcreteClass.serializer(), serialized)
    }

    @Test
    fun stubConcreteClass_test() {
        val concrete = StubConcreteClass()
        val serialized: String = Json.stringify(StubConcreteClass.serializer(), concrete)
        val parsed: StubConcreteClass = Json.parse(StubConcreteClass.serializer(), serialized)
    }


    @Test
    fun someConstructor() {
        assertStringFormAndRestored("""{"someProperty":42,"test":"Test"}""", WithSecondaryConstructor(), WithSecondaryConstructor.serializer())
    }
}

@Serializable
class WithSecondaryConstructor(var someProperty: Int) {
    var test: String = "Test"

    constructor() : this(42)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WithSecondaryConstructor) return false

        if (someProperty != other.someProperty) return false
        if (test != other.test) return false

        return true
    }

    override fun hashCode(): Int {
        var result = someProperty
        result = 31 * result + test.hashCode()
        return result
    }
}
