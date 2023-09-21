/*
 * Copyright 2017-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.polymorphic

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.jvm.*
import kotlin.test.*

class ClassDiscriminatorModeAllObjectsTest :
    JsonClassDiscriminatorModeBaseTest(ClassDiscriminatorMode.ALL_JSON_OBJECTS) {
    @Test
    fun testIncludeNonPolymorphic() = testIncludeNonPolymorphic("""{"type":"outer","inn":{"type":"inner","x":"X","e":"OptionB"},"lst":[{"type":"inner","x":"a","e":"OptionB"},{"type":"inner","x":"b","e":"OptionB"}],"lss":["foo"]}""")

    @Test
    fun testIncludePolymorphic() {
        val s = """{"type":"kotlinx.serialization.json.polymorphic.OuterNullableBox","outerBase":{"type":"kotlinx.serialization.json.polymorphic.OuterNullableImpl","""+
            """"base":{"type":"kotlinx.serialization.json.polymorphic.InnerImpl","field":42,"str":"default","nullable":null},"base2":null},"innerBase":{"type":"kotlinx.serialization.json.polymorphic.InnerImpl2","field":239}}"""
        testIncludePolymorphic(s)
    }

    @Test
    fun testIncludeSealed() {
        testIncludeSealed("""{"type":"kotlinx.serialization.Box","boxed":{"type":"container","i":{"type":"inner","x":"x","e":"OptionC"}}}""")
    }

    @Test
    fun testIncludeMixed() = testMixed("""{"type":"mixed","sb":{"type":"container","i":{"type":"inner","x":"in","e":"OptionC"}},"sc":{"type":"container","i":{"type":"inner","x":"in","e":"OptionC"}},"i":{"type":"inner","x":"in","e":"OptionC"}}""")

    @Test
    fun testIncludeCtx() =
        testContextual("""{"type":"withContextual","ctx":{"type":"CtxSerializer","a":"c","b":"d"},"i":{"type":"inner","x":"x","e":"OptionB"}}""")

    @Test
    fun testIncludeCustomDiscriminator() =
        testCustomDiscriminator("""{"type":"Cont","ec":{"message_type":"ErrorClassImpl","msg":"a"},"eci":{"message_type":"ErrorClassImpl","msg":"b"}}""")

    @Test
    fun testTopLevelPolyImpl() = testTopLevelPolyImpl(
        """{"type":"kotlinx.serialization.json.polymorphic.InnerImpl","field":42,"str":"default","nullable":null}""",
        """{"type":"container","i":{"type":"inner","x":"x","e":"OptionB"}}"""
    )

    @Test
    fun testNullable() = testNullable("""{"type":"NullableMixed","sb":null,"sc":null}""")

}

class ClassDiscriminatorModeNoneTest :
    JsonClassDiscriminatorModeBaseTest(ClassDiscriminatorMode.NONE, deserializeBack = false) {
    @Test
    fun testIncludeNonPolymorphic() = testIncludeNonPolymorphic("""{"inn":{"x":"X","e":"OptionB"},"lst":[{"x":"a","e":"OptionB"},{"x":"b","e":"OptionB"}],"lss":["foo"]}""")

    @Test
    fun testIncludePolymorphic() {
        val s = """{"outerBase":{"base":{"field":42,"str":"default","nullable":null},"base2":null},"innerBase":{"field":239}}"""
        testIncludePolymorphic(s)
    }

    @Test
    fun testIncludeSealed() {
        testIncludeSealed("""{"boxed":{"i":{"x":"x","e":"OptionC"}}}""")
    }

    @Test
    fun testIncludeMixed() = testMixed("""{"sb":{"i":{"x":"in","e":"OptionC"}},"sc":{"i":{"x":"in","e":"OptionC"}},"i":{"x":"in","e":"OptionC"}}""")

    @Test
    fun testIncludeCtx() =
        testContextual("""{"ctx":{"a":"c","b":"d"},"i":{"x":"x","e":"OptionB"}}""")

    @Test
    fun testIncludeCustomDiscriminator() = testCustomDiscriminator("""{"ec":{"msg":"a"},"eci":{"msg":"b"}}""")

    @Test
    fun testTopLevelPolyImpl() = testTopLevelPolyImpl(
        """{"field":42,"str":"default","nullable":null}""",
        """{"i":{"x":"x","e":"OptionB"}}"""
    )

    @Test
    fun testNullable() = testNullable("""{"sb":null,"sc":null}""")
}

class ClassDiscriminatorModeImplementationsTest :
    JsonClassDiscriminatorModeBaseTest(ClassDiscriminatorMode.POLYMORPHIC_AND_IMPLEMENTATIONS) {
    @Test
    fun testIncludeNonPolymorphic() = testIncludeNonPolymorphic("""{"inn":{"x":"X","e":"OptionB"},"lst":[{"x":"a","e":"OptionB"},{"x":"b","e":"OptionB"}],"lss":["foo"]}""")
    @Test
    fun testIncludePolymorphic() {
        val s = """{"outerBase":{"type":"kotlinx.serialization.json.polymorphic.OuterNullableImpl","""+
            """"base":{"type":"kotlinx.serialization.json.polymorphic.InnerImpl","field":42,"str":"default","nullable":null},"base2":null},"innerBase":{"type":"kotlinx.serialization.json.polymorphic.InnerImpl2","field":239}}"""
        testIncludePolymorphic(s)
    }

    @Test
    fun testIncludeSealed() {
        testIncludeSealed("""{"boxed":{"type":"container","i":{"x":"x","e":"OptionC"}}}""")
    }

    @Test
    fun testIncludeMixed() = testMixed("""{"sb":{"type":"container","i":{"x":"in","e":"OptionC"}},"sc":{"type":"container","i":{"x":"in","e":"OptionC"}},"i":{"x":"in","e":"OptionC"}}""")
    @Test
    fun testIncludeCtx() =
        testContextual("""{"ctx":{"a":"c","b":"d"},"i":{"x":"x","e":"OptionB"}}""")

    @Test
    fun testIncludeCustomDiscriminator() =
        testCustomDiscriminator("""{"ec":{"message_type":"ErrorClassImpl","msg":"a"},"eci":{"message_type":"ErrorClassImpl","msg":"b"}}""")

    @Test
    fun testTopLevelPolyImpl() = testTopLevelPolyImpl(
        """{"type":"kotlinx.serialization.json.polymorphic.InnerImpl","field":42,"str":"default","nullable":null}""",
        """{"type":"container","i":{"x":"x","e":"OptionB"}}"""
    )

    @Test
    fun testNullable() = testNullable("""{"sb":null,"sc":null}""")

    @Serializable
    sealed interface X

    @Serializable
    @SerialName("x_y")
    data class Y(val i: Int) : X

    // This test is specifically plugin-oriented. When we invoke only Y.serializer(), initializer of X.serializer() = SealedClassSerializer(...) is not called
    // and (s.descriptor as? PluginGeneratedSerialDescriptor)?.markAsInheritor() line there is not called.
    // Therefore, the only way of correctly marking Y as inheritor is plugin.
    @Test
    fun testOnlySubclass() {
        val y = Y(42)
        if (KotlinVersion.CURRENT >= KotlinVersion(2, 0, 0))
            doTest("""{"type":"x_y","i":42}""", y)
    }
}

// FIXME: add tests for value classes when #2288 is fixed
